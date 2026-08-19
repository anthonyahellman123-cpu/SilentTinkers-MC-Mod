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
statistics, and traits must survive conversion into a Tinkers part and remain
available after normal Tinkers assembly and modification.

## First vertical slice

1. Read any valid Silent Gear compound alloy ingot. **Complete**
2. Convert its composition into a canonical, versioned representation. **Complete**
3. Preserve that representation through melting and casting. **Complete**
4. Create one universal Tinkers pick head carrying the representation. **Complete**
5. Apply Silent Gear's evaluated stats to a completed test tool. **Implemented; in-game validation pending**
6. Forward thresholded native Tinkers/addon traits. **Implemented; in-game validation pending**

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
  for the finished ingot's evaluated head stats. This automatically respects
  its alloy weighting, grade, starcharge, and compatible addon calculations.
- The evaluated snapshot travels in the same bounded payload and is encoded in
  the Tinkers material variant on the cast part. The composite modifier replaces
  the neutral material baseline with those stats during tool construction.
- For ingredients that also exist in Tinkers or an installed Tinkers addon, the
  modifier asks Tinkers' live material registry for the real head traits. The
  addon that owns a trait therefore remains responsible for its implementation.
  Resolution tries the exact ID, the built-in `tconstruct` equivalent, and then
  a cross-addon path match only when that path is unique in the material registry.

## Current in-game validation

The alpha produces both a diagnostic `Composite Alloy Sample` and a real
`tconstruct:pick_head`. The sample now displays the exact Silent Gear stats
captured before melting; the completed tool applies the same snapshot.

1. Install Silent Tinkers with the compatibility baseline mods listed above.
2. Make a `silentgear:alloy_ingot` through Silent Gear's normal alloy system.
3. Melt one ingot in a Tinkers smeltery or melter at 1200 C or hotter.
4. Pour one ingot (90 mB) into an empty casting table with no cast. Hover the
   resulting sample; its tooltip should list the canonical material
   IDs, percentages, composition fingerprint, and a gold "Silent Gear evaluated
   stats" section. If that stats section is absent, do not continue: the runtime
   Silent Gear API bridge needs adjustment for the installed version.
5. For the real-part test, put a reusable pick head cast on the table and pour
   two ingots (180 mB). The output should be a purple `tconstruct:pick_head`
   whose `Material` NBT begins with
   `silenttinkers:composite_alloy#v1.`.
6. Build a normal Tinkers pickaxe with that head. Its head contribution now uses
   the captured Silent Gear durability, mining speed, melee damage, attack speed,
   and harvest tier, while its handle and binding still use normal Tinkers math.
7. Check the completed tool's modifiers. Each ingredient gets zero, one, two, or
   all matching native head traits according to its alloy percentage and the
   server config in `serverconfig/silenttinkers-server.toml`.

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
requires the short in-game validation above.
