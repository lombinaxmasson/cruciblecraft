# Material data units

The `thermal.melting_point` and `thermal.boiling_point` fields are degrees
Celsius. Runtime material temperatures remain Celsius; integrations that
require Kelvin must convert only at their boundary.

Material prefix quantities use CrucibleCraft material units, with exactly 144
units per ingot. GT6 import code must use `GT6ImportUnits` to convert from GT6
U (648,648,000 U per ingot) so fractional units and overflow are rejected.

The ore-processing intermediates `crushed_ore`, `washed_crushed_ore`, and
`centrifuged_crushed_ore`, plus `purified_dust`, are intentionally normalized
to 144 units in CrucibleCraft. `tiny_crushed_ore` is likewise normalized to 16
units. These are gameplay yield stages rather than direct GT6 prefix-amount
translations, so their GT6 amounts (respectively 162/180/198/176/18 after U
conversion, depending on the prefix) are not used as runtime item units.
Storage `block` uses the mapped `blockIngot`/`blockGem` domain and remains nine
ingots (1,296 units); GT6's unrelated compatibility prefix named `block` is
not a CrucibleCraft material form.

## Factual and registered forms

The structural fields in each material JSON resolve to `MaterialDefinition.forms()`.
They record every GT6-registered prefix that maps to one of CrucibleCraft's 42
prefixes; recipe selection does not rewrite this factual set.

`metadata_only` is true exactly when that GT6/CrucibleCraft prefix intersection
is empty. Metadata-only materials must have no factual forms, and non-metadata
materials must have at least one.

Actual items use the committed
`data/cruciblecraft/material_registration_gate.json`. `MaterialCatalog.registeredForms()`
is the recipe/item/model/tag-facing set. The gate is generated from the
committed L1b selected-recipe operand projection, with explicit compatibility
retention for forms that existed before the gate was introduced. Runtime code
never recomputes the gate from recipe dumps.

The top-level `compatibility_forms` object is audit metadata only. Its legacy
baseline is recomputed from
`tools/gt6_material_activation_policy.json:records[].pre_gate_registered_forms`;
the builder never reads the previous gate, so retained forms cannot recursively
ratchet into the next build. Runtime loads only the already-merged `materials`
object.
