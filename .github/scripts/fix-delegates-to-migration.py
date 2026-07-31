from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

ACCESS_TESTS = [
    ROOT / "src/test/java/org/tavall/dependency/access/DependencyAccessFiveDependencyIntegrationTest.java",
    ROOT / "src/test/java/org/tavall/dependency/access/DependencyAccessProductionIntegrationTest.java",
    ROOT / "src/test/java/org/tavall/dependency/access/DependencyAccessGrantProcessorTest.java",
]
OLD_ANNOTATION_TEST = ROOT / "src/test/java/org/tavall/dependency/annotations/DelegatesToInterfaceTest.java"
NEW_ANNOTATION_TEST = ROOT / "src/test/java/org/tavall/dependency/annotations/DelegatesToTest.java"
HELPER = ROOT / "src/main/java/org/tavall/dependency/injection/helpers/DependencyInjectorHelper.java"
LEGACY_FIXTURE = ROOT / "src/test/java/org/tavall/dependency/injection/helpers/legacyfixtures/LegacyDelegatingService.java"
LEGACY_ANNOTATION = ROOT / "src/main/java/org/tavall/dependency/annotations/DelegatesToInterface.java"
HELPER_TEST = ROOT / "src/test/java/org/tavall/dependency/injection/helpers/DependencyInjectorHelperTest.java"


def update_access_test(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    text = text.replace(
        "DelegatesToInterface delegatesToInterface =",
        "DelegatesTo delegatesTo =",
    )
    text = text.replace(
        "getAnnotation(DelegatesToInterface.class)",
        "getAnnotation(DelegatesTo.class)",
    )
    text = text.replace("delegatesToInterface", "delegatesTo")
    text = text.replace("delegatesTo.value().getName()", "delegatesTo.value()[0].getName()")
    text = text.replace("delegatesTo.value())", "delegatesTo.value()[0])")
    path.write_text(text, encoding="utf-8")


def migrate_annotation_test() -> None:
    text = OLD_ANNOTATION_TEST.read_text(encoding="utf-8")
    text = text.replace("class DelegatesToInterfaceTest", "class DelegatesToTest")
    text = text.replace(
        "arrayOnlyAnnotationRegistersInterfacesInDeclaredOrder",
        "arrayAnnotationRegistersDependencyTokensInDeclaredOrder",
    )
    text = text.replace(
        "mixedAnnotationDeduplicatesAndSkipsInterfacesTheConcreteDoesNotImplement",
        "mixedAnnotationDeduplicatesAndSkipsUnassignableDependencyTokens",
    )
    NEW_ANNOTATION_TEST.write_text(text, encoding="utf-8")
    OLD_ANNOTATION_TEST.unlink()


def validate() -> None:
    required = [
        ROOT / "src/main/java/org/tavall/dependency/annotations/DelegatesTo.java",
        LEGACY_ANNOTATION,
        HELPER,
        LEGACY_FIXTURE,
        HELPER_TEST,
        NEW_ANNOTATION_TEST,
    ]
    missing = [str(path.relative_to(ROOT)) for path in required if not path.exists()]
    if missing:
        raise RuntimeError("Migration did not create required files: " + ", ".join(missing))

    allowed_legacy_paths = {
        LEGACY_ANNOTATION,
        HELPER,
        LEGACY_FIXTURE,
        HELPER_TEST,
    }
    stale = []
    for path in (ROOT / "src").rglob("*.java"):
        if path in allowed_legacy_paths:
            continue
        if "DelegatesToInterface" in path.read_text(encoding="utf-8"):
            stale.append(str(path.relative_to(ROOT)))
    if stale:
        raise RuntimeError("Unexpected legacy annotation references: " + ", ".join(stale))


for access_test in ACCESS_TESTS:
    update_access_test(access_test)

migrate_annotation_test()
validate()
