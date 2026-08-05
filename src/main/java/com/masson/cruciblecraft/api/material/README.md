# Material startup API

`MaterialPrefixRegistrationEvent` and `MaterialRegistrationEvent` are
convenience events posted from CrucibleCraft's own `FMLConstructModEvent`
callback. Mod construction is serial: only listeners that already exist can
receive those posts.

An addon that uses either event must declare an explicit NeoForge dependency
ordering it **before** `cruciblecraft`. An unordered addon, or one ordered after
CrucibleCraft, has no delivery guarantee. This is an API constraint, not a
loader-version workaround.

Preferred integration:

1. Establish explicit startup ordering.
2. Register prefixes before any material definitions.
3. Call `MaterialPrefixCatalog.addStartupPrefix` and
   `MaterialCatalog.addStartupMaterial` from that ordered startup hook.
4. Treat a `false` return as a duplicate identity and fail addon startup.

Both catalogs freeze after startup. Runtime mutation and listener-based
fallback registration are unsupported.
