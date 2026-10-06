# Provided Dependencies

## Purpose

Provided dependencies let Tavall DI own the construction instructions for an object whose concrete class Tavall does not own or cannot instantiate through the normal no-argument `@DelegatesTo` path.

The runtime authority does not change:

```text
dependency token
    -> DependencyMap
    -> IDependencyMetaData
    -> metadata-owned instance
```

There is no third-party container, provider cache, or alternate lookup path.

## Ownership Model

Use the construction path that matches who owns the object:

| Path | Use when |
|---|---|
| `@DelegatesTo` | Tavall owns the concrete class and Tavall DI may construct it directly. |
| `@ProvidedDependency` | Tavall owns a small factory, while the produced class belongs to a third-party library or otherwise needs custom assembly. |
| `registerInstance` | Another runtime already constructed the object and Tavall DI only needs to bind it. |

## Factory Contract

A provider implements `IDependencyFactory<T>`:

```java
public interface IDependencyFactory<T> {
    T create(IDependencyMap dependencyMap);
}
```

The owning map is passed into the factory. Providers should resolve required Tavall dependencies from that map instead of reading the global map directly.

## Annotation Pattern

`@ProvidedDependency` has one value: the dependency tokens exposed by the produced instance.

```java
@ProvidedDependency({DataSource.class, HikariDataSource.class})
public final class HikariDataSourceFactory
        implements IDependencyFactory<HikariDataSource> {

    @Override
    public HikariDataSource create(IDependencyMap dependencies) {
        DatabaseConfiguration configuration =
                dependencies.getInstance(DatabaseConfiguration.class);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(configuration.jdbcUrl());
        hikariConfig.setUsername(configuration.username());
        hikariConfig.setPassword(configuration.password());
        return new HikariDataSource(hikariConfig);
    }
}
```

The first annotation value is the primary token. Any remaining values are aliases. Every token resolves to the same metadata object and the same instance.

The provider class itself is construction metadata and is not registered as the produced dependency.

## Bootstrap Order

Normal `@DelegatesTo` dependencies are registered and initialized first. Provided dependency factories run afterward. This lets a provider consume ordinary Tavall configuration and services during construction.

A provider should remain small and deterministic. It should assemble the foreign object, not become a second service layer.

## Manual Registration

Infrastructure code may register a factory without annotation scanning:

```java
DataSource dataSource = dependencyMap.registerFactory(
        DataSource.class,
        dependencies -> createDataSource(
                dependencies.getInstance(DatabaseConfiguration.class)),
        HikariDataSource.class
);
```

The additional runtime tokens are aliases. Factory registration validates the produced instance against the primary token and every alias before any metadata is published.

## Runtime Behavior

Once registration completes, consumers use ordinary Tavall DI access:

```java
DataSource dataSource = getDependencyMap().getInstance(DataSource.class);
```

Consumers do not know whether the dependency came from reflection, a factory, or a preconstructed instance.

Replacement also remains ordinary Tavall DI behavior. Replacing an instance through one provided token updates the metadata shared by all aliases.

## Failure Rules

Fail registration when:

- `@ProvidedDependency` declares no usable tokens;
- the annotated provider does not implement `IDependencyFactory`;
- a provider also declares `@DelegatesTo`;
- the factory returns `null`;
- the produced instance does not satisfy the primary token;
- the produced instance does not satisfy any declared alias;
- the provider cannot be constructed through its no-argument constructor.

Do not inspect third-party constructors automatically. The provider exists specifically to keep third-party construction explicit and stable across library upgrades.
