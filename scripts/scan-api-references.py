#!/usr/bin/env python3
"""Find what a built mod jar needs from Minecraft (or a loader API) that a newer version lacks.

From 26.1 on Minecraft ships unobfuscated and publishes no mappings, so a 26.x mod jar names every
class, field and method it uses by its real name. This reads those references out of the jar's
constant pools - plus what its mixins bind by string, which no constant pool records: @Mixin
targets, injector `method` names with the handler's captured arguments, @Shadow, @Accessor and
@Invoker members - and resolves each one against two sets of jars: the version the mod was built
for (the control, which must come back clean or the scan itself is wrong) and the one being ported to.

    python scripts/scan-api-references.py --mod mightyarchitect-fabric.jar \
        --old client-26.2.jar --new client-26.3.jar
    python scripts/scan-api-references.py --mod mightyarchitect-neoforge.jar --prefix net/neoforged/ \
        --old neoforge-26.2-universal.jar --new neoforge-26.3-universal.jar

Jars nested under META-INF/jars/ (Fabric API's bundle) are indexed too. Not covered: `@At(target)`
strings, members a loader patches into Minecraft classes, new abstract methods on types the mod
extends, compile-time constants (26.3 renumbered every key and mouse button and no reference
changed), libraries leaving the classpath (GLFW, in 26.3), resources, and behaviour - 26.3 also
changed what BlockState.CODEC writes, which only a unit test pinning the file format could catch.
The compiler and the test suites still own those; this only makes the first compile a short one.
"""
import argparse
import io
import re
import struct
import sys
import zipfile
from collections import defaultdict

MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;"
SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;"
INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;"
ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;"
INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;"
CALLBACK = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo"
ANNOTATION_TABLES = ("RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations")


def parse_class(data):
    """The parts of a class file this needs: hierarchy, declared members, references, annotations."""
    u2 = lambda at: struct.unpack_from(">H", data, at)[0]
    u4 = lambda at: struct.unpack_from(">I", data, at)[0]
    pool, pos, i = [None] * u2(8), 10, 1
    while i < len(pool):
        tag = data[pos]
        if tag == 1:
            end = pos + 3 + u2(pos + 1)
            pool[i], pos = data[pos + 3:end].decode("utf-8", "replace"), end
        elif tag in (7, 8, 16, 19, 20):
            pool[i], pos = (tag, u2(pos + 1)), pos + 3
        elif tag in (9, 10, 11, 12, 17, 18):
            pool[i], pos = (tag, u2(pos + 1), u2(pos + 3)), pos + 5
        elif tag in (3, 4):
            pos += 5
        elif tag in (5, 6):
            pos, i = pos + 9, i + 1
        elif tag == 15:
            pos += 4
        else:
            raise ValueError(f"unknown constant pool tag {tag}")
        i += 1
    class_name = lambda index: pool[pool[index][1]]

    def element(at):
        tag, at = chr(data[at]), at + 1
        if tag == "[":
            values, count, at = [], u2(at), at + 2
            for _ in range(count):
                value, at = element(at)
                values.append(value)
            return values, at
        if tag == "@":
            return annotation(at)
        if tag == "e":
            return pool[u2(at + 2)], at + 4
        value = pool[u2(at)]
        return (value if isinstance(value, str) else None), at + 2

    def annotation(at):
        kind, values, pairs, at = pool[u2(at)], {}, u2(at + 2), at + 4
        for _ in range(pairs):
            name = pool[u2(at)]
            values[name], at = element(at + 2)
        return (kind, values), at

    def annotations(at):
        """Reads one attribute table; returns the annotations in it and the offset after it."""
        found, count, at = [], u2(at), at + 2
        for _ in range(count):
            name, length, at = pool[u2(at)], u4(at + 2), at + 6
            if name in ANNOTATION_TABLES:
                cursor = at + 2
                for _ in range(u2(at)):
                    item, cursor = annotation(cursor)
                    found.append(item)
            at += length
        return found, at

    pos += 2  # access flags
    this, parent, interface_count = u2(pos), u2(pos + 2), u2(pos + 4)
    interfaces = [class_name(u2(pos + 6 + 2 * k)) for k in range(interface_count)]
    pos += 6 + 2 * interface_count
    members = {"field": {}, "method": {}}
    for kind in ("field", "method"):
        count, pos = u2(pos), pos + 2
        for _ in range(count):
            key = (pool[u2(pos + 2)], pool[u2(pos + 4)])
            members[kind][key], pos = annotations(pos + 6)
    class_annotations, pos = annotations(pos)

    classes, refs = set(), set()
    for entry in pool:
        if isinstance(entry, tuple) and entry[0] == 7:
            classes.add(pool[entry[1]])
        elif isinstance(entry, tuple) and entry[0] in (9, 10, 11):
            name_and_type = pool[entry[2]]
            refs.add(("field" if entry[0] == 9 else "method", class_name(entry[1]),
                      pool[name_and_type[1]], pool[name_and_type[2]]))
    return {"name": class_name(this), "super": class_name(parent) if parent else None,
            "interfaces": interfaces, "field": members["field"], "method": members["method"],
            "annotations": class_annotations, "classes": classes, "refs": refs}


def class_files(jar):
    """Every class in a jar, following jars nested under META-INF/jars/."""
    for name in jar.namelist():
        if name.endswith(".class"):
            yield name[:-6], jar
        elif name.startswith("META-INF/jars/") and name.endswith(".jar"):
            yield from class_files(zipfile.ZipFile(io.BytesIO(jar.read(name))))


def parameters(descriptor):
    return re.findall(r"\[*(?:L[^;]+;|[BCDFIJSZ])", descriptor[1:descriptor.index(")")])


def simple(name):
    return name.rsplit("/", 1)[-1]


class Index:
    def __init__(self, paths):
        self.sources, self.cache = {}, {}
        for path in paths:
            for name, jar in class_files(zipfile.ZipFile(path)):
                self.sources.setdefault(name, jar)

    def load(self, name):
        if name not in self.cache:
            jar = self.sources.get(name)
            self.cache[name] = parse_class(jar.read(name + ".class")) if jar else None
        return self.cache[name]

    def resolve(self, owner, kind, name, descriptor, seen=None):
        """ok, missing, or external when the hierarchy leaves these jars before the member is found."""
        seen = set() if seen is None else seen
        cls = self.load(owner)
        if cls is None or owner in seen:
            return "external" if cls is None else "missing"
        seen.add(owner)
        if (name, descriptor) in cls[kind]:
            return "ok"
        inherited = [self.resolve(parent, kind, name, descriptor, seen)
                     for parent in [cls["super"], *cls["interfaces"]] if parent]
        return "ok" if "ok" in inherited else "external" if "external" in inherited else "missing"

    def declared(self, owner, kind, name):
        cls = self.load(owner)
        return sorted(d for (n, d) in cls[kind] if n == name) if cls else []


def requirements(mod_path, prefixes):
    """What the mod needs: {requirement: mod classes that need it}, and its mixin classes."""
    needs, mixins = defaultdict(set), []
    for entry, jar in class_files(zipfile.ZipFile(mod_path)):
        cls = parse_class(jar.read(entry + ".class"))
        mentioned = set(cls["classes"])
        for kind, owner, name, descriptor in cls["refs"]:
            mentioned.update(re.findall(r"L([^;]+);", descriptor))
            if owner.startswith(prefixes):
                needs[(kind, owner, name, descriptor)].add(simple(cls["name"]))
        for name in mentioned:
            name = name.lstrip("[").removeprefix("L").removesuffix(";") if name.startswith("[") else name
            if name.startswith(prefixes):
                needs[("class", name, "", "")].add(simple(cls["name"]))
        for kind, values in cls["annotations"]:
            if kind == MIXIN:
                targets = [d[1:-1] for d in values.get("value", [])]
                targets += [t.replace(".", "/") for t in values.get("targets", [])]
                mixins.append((cls, [t for t in targets if t.startswith(prefixes)]))
    return needs, mixins


def mixin_problems(index, cls, targets):
    """Members a mixin binds by string, which no constant pool records: yields (what, problem)."""
    mixin = simple(cls["name"])
    for target in targets:
        if index.load(target) is None:
            yield f"{mixin} -> {target}", "target class is gone"
    targets = [t for t in targets if index.load(t)]
    for kind in ("field", "method"):
        for (name, descriptor), found in cls[kind].items():
            for annotation, values in found:
                if annotation == SHADOW:
                    prefix = values.get("prefix") or "shadow$"
                    real = name.removeprefix(prefix)
                    if all(index.resolve(t, kind, real, descriptor) == "missing" for t in targets):
                        yield f"{mixin} @Shadow {real} {descriptor}", "no such member"
                elif annotation in (ACCESSOR, INVOKER):
                    bare = re.sub(r"^(get|set|is|invoke|call)(?=[A-Z])", "", name)
                    real = values.get("value") or (bare if bare.isupper() else bare[:1].lower() + bare[1:])
                    wanted = "field" if annotation == ACCESSOR else "method"
                    if not any(index.declared(t, wanted, real) for t in targets):
                        yield f"{mixin} @{simple(annotation)[:-1]} {real}", "no such member"
                specs = [re.match(r"^(?:L[^;]+;)?([^(\s]+?)(\(.*)?$", s) for s in values.get("method") or []]
                if not specs or not all(specs) or any("*" in s[0] for s in specs):
                    continue
                wanted = [d for s in specs for t in targets for d in index.declared(t, "method", s[1])
                          if s[2] in (None, d)]
                what = f"{mixin} -> {', '.join(simple(t) for t in targets)}.{' | '.join(s[0] for s in specs)}"
                handler = parameters(descriptor)
                captured = handler[:next((k for k, p in enumerate(handler) if p.startswith(CALLBACK)), len(handler))]
                if not wanted:
                    yield what, "no such method"
                elif annotation == INJECT and captured and all(parameters(d) != captured for d in wanted):
                    yield what, (f"handler takes ({', '.join(simple(p).rstrip(';') for p in captured)}) "
                                 f"but the target is {'; '.join(wanted)}")


def scan(index, needs, mixins):
    results = {}
    for requirement in needs:
        kind, owner, name, descriptor = requirement
        if index.load(owner) is None:
            results[requirement] = "missing"
        elif kind != "class":
            results[requirement] = index.resolve(owner, kind, name, descriptor)
        else:
            results[requirement] = "ok"
    problems = {what: why for cls, targets in mixins for what, why in mixin_problems(index, cls, targets)}
    return results, problems


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--mod", required=True, help="the built mod jar to read requirements from")
    parser.add_argument("--old", required=True, nargs="+", help="jars of the version the mod was built for")
    parser.add_argument("--new", required=True, nargs="+", help="jars of the version being ported to")
    parser.add_argument("--prefix", nargs="+", default=["net/minecraft/", "com/mojang/"],
                        help="package prefixes that count as the API under test")
    args = parser.parse_args()

    needs, mixins = requirements(args.mod, tuple(args.prefix))
    old, new = Index(args.old), Index(args.new)
    # A prefix as wide as com/mojang/ also matches libraries (DFU, Brigadier) that ship beside the
    # game rather than in it. What the old jars do not contain at all was never under test.
    outside = [r for r in needs if old.load(r[1]) is None]
    needs = {r: users for r, users in needs.items() if old.load(r[1]) is not None}
    mixins = [(cls, [t for t in targets if old.load(t)]) for cls, targets in mixins]
    mixins = [(cls, targets) for cls, targets in mixins if targets]
    (old_results, old_problems), (new_results, new_problems) = scan(old, needs, mixins), scan(new, needs, mixins)

    print(f"{len(needs)} references into {', '.join(args.prefix)} and {len(mixins)} mixins "
          f"({len(outside)} more name classes outside the given jars and are not checked)")
    for label, results, problems in (("old (control)", old_results, old_problems), ("new", new_results, new_problems)):
        tally = {status: sum(1 for s in results.values() if s == status) for status in ("ok", "missing", "external")}
        print(f"  {label:<14} ok={tally['ok']}  missing={tally['missing']}  "
              f"unchecked (inherited from outside these jars)={tally['external']}  mixin problems={len(problems)}")

    broken = sorted(r for r in needs if old_results[r] == "ok" and new_results[r] != "ok")
    gone = {r[1] for r in broken if r[0] == "class"}
    print(f"\nresolves on old but not on new: {len(broken)} references, {len(new_problems)} mixin bindings")
    for kind, owner, name, descriptor in broken:
        users = ", ".join(sorted(needs[(kind, owner, name, descriptor)]))
        if kind == "class":
            moved = [n for n in new.sources if simple(n) == simple(owner)]
            print(f"  CLASS  {owner}  ->  {', '.join(moved) if moved else 'gone'}   [{users}]")
        elif owner not in gone:
            print(f"  {kind.upper():<6} {simple(owner)}.{name} {descriptor}   [{users}]")
            print(f"           now: {'; '.join(new.declared(owner, kind, name)) or 'no member of that name'}")
    for what, why in sorted(new_problems.items()):
        if what not in old_problems:
            print(f"  MIXIN  {what}: {why}")

    control_failures = [r for r, status in old_results.items() if status == "missing"] + list(old_problems)
    if control_failures:
        print(f"\nCONTROL FAILED: {len(control_failures)} requirement(s) do not resolve on the version the mod "
              f"was built for, so this report cannot be trusted:", *control_failures[:10], sep="\n  ")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())