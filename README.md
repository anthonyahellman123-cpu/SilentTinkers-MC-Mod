# Silent Tinkers

Forge 1.20.1 compatibility addon that carries Silent Gear's detailed ingot
identity into Tinkers' Construct's multipart tool system.

## Compatibility baseline

- Minecraft 1.20.1
- Forge 47.4.10
- Tinkers' Construct 3.11.2.166
- Mantle 1.11.104
- Silent Gear 3.6.7

## Design rule

The processed ingot is the source of truth. Its material composition, grade,
statistics, traits, and eventually its visible identity should survive conversion
into a Tinkers part instead of being re-authored as a separate fake material.

## First vertical slice

1. Read valid Silent Gear material/alloy identity. **Complete**
2. Convert composition into a canonical, versioned representation. **Complete**
3. Preserve that representation through melting and casting. **Complete**
4. Create one universal Tinkers pick head carrying the representation. **Complete**
5. Apply Silent Gear's evaluated stats to a completed Tinkers tool. **Validated in game**
6. Preserve normal Tinkers multipart stat math around the dynamic head. **Validated in game**
7. Forward thresholded native Tinkers/addon traits. **Implemented; broader in-game validation pending**
8. Preserve the original source item's visual identity metadata across conversion. **Implemented; renderer pending**

Equivalent ratios share the same compact fingerprint. Runtime-created alloys do
not register new fluids or materials globally; one carrier fluid holds a bounded
composition payload instead.

## Integration architecture

- `SilentGearAlloyReader` reads the real `silentgear:alloy_ingot` material list,
  including the legacy save format, and converts it to a canonical composition.
- A single addon-owned molten-alloy fluid carries that composition in its
  `FluidStack` tag. Different alloys therefore do not require globally
  registering hundreds of fluids or materials.
- Custom melting and casting recipes copy the tag from ingot to fluid and from
  fluid to the cast result. Tinkers 3.11 exposes both inventory-aware melting
  output and casting-fluid NBT, so this path preserves identity through tanks.
- At melt time, `SilentGearStatReader` asks Silent Gear's public material API
  for the finished material's evaluated head stats. This automatically respects
  native Silent Gear evaluation instead of reimplementing its formulas.
- The evaluated snapshot travels in the same bounded payload and is encoded in
  the Tinkers material variant on the cast part. The composite modifier replaces
  the neutral material baseline with those stats during tool construction.
- For ingredients that also exist in Tinkers or an installed Tinkers addon, the
  modifier asks Tinkers' live material registry for the real head traits. The
  addon that owns a trait therefore remains responsible for its implementation.

## Visual identity relay

SilentTinkers now records what the material actually was before conversion rather
than assigning a replacement color by material name. `SourceVisualIdentity`
captures the source item registry ID and, when reasonably sized, a defensive copy
of the source stack tag. That matters for dynamic Silent Gear alloys whose NBT may
influence how the original item is presented.

The source identity currently follows this path:

`source item` → `molten composite payload` → `diagnostic sample / cast part`.

The source item ID is also encoded into the synthetic Tinkers material variant,
so normal Tinkers assembly carries that origin into the finished tool even though
Tinkers does not copy arbitrary part NBT into its tool NBT. Development tooltips
show the recovered visual source ID so this boundary can be tested independently
before rendering is changed.

The renderer itself is intentionally a later step. It should ask the original
source item's client rendering data what it looked like and project that visual
identity onto the composite fluid/part frame. SilentTinkers should not maintain a
hard-coded table such as `elementium = pink`; the source ecosystem remains the
authority for both behavior and appearance.

## Validated Elementium path

A Botania Elementium test now proves the complete numeric bridge:

`botania:elementium_ingot`
→ Silent Gear material `silentcompat:elementium`
→ dynamic molten composite payload
→ synthetic composite pick head
→ normal Tinkers pickaxe assembly
→ dynamic head stats applied by `CompositeAlloyModifier`.

The encoded Silent Gear head snapshot was:

- durability 720
- mining speed 6.2
- melee damage 2.0
- attack speed 0.0
- harvest tier diamond

The completed pickaxe kept that encoded variant, bound the composite modifier,
and finished with normal Tinkers multipart math at durability 540, mining speed
4.96, melee damage 3.6000001, attack speed 1.2, and Diamond harvest tier.
The runtime log emitted both `[SilentTinkers:COMPOSITE_STATS_APPLIED]` and
`[SilentTinkers:COMPOSITE_TOOL_OBSERVED]`, confirming the same result from the
server-side stat hook and the finished client tool.

## Current in-game validation

1. Install Silent Tinkers with the compatibility baseline mods listed above.
2. Use a discovered one-unit Silent Gear material form such as a supported
   ingot/gem/crystal, or a normal Silent Gear compound alloy ingot.
3. Melt the supported material in a Tinkers smeltery or melter.
4. Pour into the diagnostic sample or a reusable pick-head cast. New casts should
   show `Visual source: <original item id>` in the development tooltip.
5. Assemble the resulting composite pick head into a normal Tinkers pickaxe. New
   variants also carry the source item ID into the finished-tool diagnostic.
6. `/silenttinkers status` reports scan health, bridge counts, composite hook
   binding, and whether dynamic assembled-tool stats have actually executed in
   the current server session.
7. `/silenttinkers inspect` reports how SilentTinkers currently interprets the
   material item held in the player's main hand.

The first runtime bridge intentionally does **not** treat ore blocks, storage
blocks, nuggets, plates, planks, or arbitrary components as one ingot. Only
safe one-unit material forms are exposed until unit-aware/tag-driven conversion
is implemented. This prevents an ore or storage block from accidentally melting
for the value of a single ingot.

## Server diagnostics

Silent Tinkers publishes an immutable material-plan snapshot after each complete
scan. Startup emits `[SilentTinkers:STARTUP_SUMMARY]` with a deterministic short
plan fingerprint. A matching fingerprint across restarts means the logical
bridge plan is unchanged.

`/silenttinkers status` currently distinguishes three different ideas that are
important during pack debugging:

- the discovery/evaluation plan exists,
- the static composite Tinkers hook is bound,
- a real assembled composite tool has executed the dynamic stat hook this
  server session.

That last state is intentionally a runtime latch rather than an assumption: a
healthy fresh server can say `NOT YET OBSERVED` until somebody actually builds
or loads a composite tool that causes Tinkers to rebuild its stats.

Default trait gates are 25% for the first trait, 50% for the first two traits,
and 75% for the complete trait package. Set all three values to `0` for the
deliberately unbalanced mode where every present ingredient receives its full
native package. Ingredients with no matching Tinkers material are skipped for
traits but still contribute to Silent Gear's evaluated numeric stats.

Starcharge an alloy ingot before melting to test the charged path. Silent Gear's
evaluated charged stats and the charge level are both preserved through fluid,
part, and final tool identity. Directly inserting a Tinkers part into Silent
Gear's starcharger is not yet supported because Silent Gear rejects items that
are not its own material instances before charging begins.

Do not mix a second composition into a tank already holding composite alloy.
The intended behavior is for differently tagged fluid stacks to remain
separate; this still needs an in-game pack test against every supported tank and
pipe implementation.

## Automated build

Every push runs `./gradlew build` on Java 17 in GitHub Actions and uploads the
reobfuscated jar as a workflow artifact. A green build proves compilation,
resource processing, tests, and Forge reobfuscation; Minecraft behavior still
requires in-game validation for new runtime paths.
