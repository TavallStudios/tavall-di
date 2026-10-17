# Provided Dependencies

`@ProvidedDependency` binds an explicitly constructed foreign object to the same metadata and owning map as ordinary Tavall dependencies. Use `@DelegatesTo` when Tavall owns the implementation, and `registerInstance` in runtime/bootstrap infrastructure when another owner already constructed it.

A factory implements `IDependencyFactory<T>` with `T create()`. Factories and consumers obtain inputs through the existing typed `DependencyAccess` pattern and named getters. They do not accept or call `IDependencyMap` or `DependencyMap` directly.

```java
@ProvidedDependency({DataSource.class, HikariDataSource.class})
public final class HikariDataSourceFactory
        implements IDependencyFactory<HikariDataSource>,
                   DependencyAccess<DatabaseConfiguration> {
    @Override
    public HikariDataSource create() {
        DatabaseConfiguration configuration = getConfiguration();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(configuration.jdbcUrl());
        config.setUsername(configuration.username());
        config.setPassword(configuration.password());
        return new HikariDataSource(config);
    }

    private DatabaseConfiguration getConfiguration() {
        return getInstance();
    }
}

@DelegatesTo
public final class QueryService implements DependencyAccess<DataSource> {
    private DataSource getDataSource() {
        return getInstance();
    }
}
```

For multiple inputs, use the existing expanded declaration, such as `DependencyAccess<DatabaseConfiguration, PoolSettings>`, and named getters returning `getInstance().databaseConfiguration()` and `getInstance().poolSettings()`. Source lowering generates the typed access class for a factory annotated with `@ProvidedDependency`, just as it does for `@DelegatesTo` consumers. The expanded declaration requires the existing lowering task before Java compilation.

The first annotation token is primary; remaining tokens are aliases. They share one metadata object and one instance. Consumers do not need a separate third-party lookup path.

Bootstrap publishes ordinary bindings before initializing them, then constructs factories after their declared typed or injected prerequisites are initialized. A consumer requiring a provided object during injection or lifecycle initialization receives it before its own initialization. Provider chains and the produced object’s own declared inputs are initialized before lifecycle callbacks; construction cycles fail explicitly. A typed token already owned by the same object is self-access, not a cycle. An undeclared lookup inside arbitrary user code is not a bootstrap dependency declaration.

Factories remain small construction owners. Runtime/bootstrap infrastructure may call `registerFactory(DataSource.class, new HikariDataSourceFactory(), HikariDataSource.class)` on the owning map. A factory using live typed getters with an isolated map extends the existing `GenerationOwnedDependencyAccess` base; it and its consumers retain the same typed getters. The map binds that factory to its existing generation lifecycle, and clearing the map retires its access. There is no second container or provider cache.

Registration rejects empty tokens, non-concrete factories, missing `IDependencyFactory`, simultaneous `@DelegatesTo`, duplicate provider tokens, null results and incompatible primary/alias types. Foreign constructors are never inferred. A failed product initialization restores displaced primary/alias metadata and removes its newly bound factory. A failed bootstrap rolls back registrations made by that scan so setup can be retried while unrelated bindings remain intact. Factories own cleanup of partially constructed resources when their construction fails. Once registered, instances use the ordinary map lifecycle and alias replacement behavior.

The integration suite compiles expanded factory and consumer declarations and constructs a JDK `HttpClient` through typed access without making network requests. It also covers initialized configuration prerequisites, injected consumers, alias identity and replacement, invalid declaration discovery, and isolated-map retirement.

Expanded self-access retains the originating owner while traversing the generated view. Generated view initialization does not turn a self token into a dependency cycle. Repeating setup after partial token loss reuses existing ordinary/provider metadata and repairs aliases without reconstructing live products. Rollback removes aliases added during a failed repair without retiring a generation that was already owned before the pass.

Manual zero-argument lambda factories remain replaceable at the same call site. Pure named, anonymous and lambda construction suppliers stay with produced metadata. A factory-class binding is used when the factory declares `@ProvidedDependency`, generation ownership, injected fields or lifecycle callbacks; pure construction alone does not create a class singleton. The pre-initialization hook is a separate bootstrap-infrastructure method, so the existing manual registration signature retains its varargs contract.

Map-created metadata retains the map that owns it, so injected fields on factories and products resolve in that scope. Foreign products need no access-interface implementation for metadata-owned injection. Generation-owned factory products use the existing generation registration path and aliases. Repairing a missing provider-class binding preserves the live product and metadata, rebinds the factory construction strategy, and restores that strategy if a later bootstrap step fails. Ordinary and provided declarations cannot compete for the same token.
