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
4. Create one universal Tinkers tool part carrying the representation. **Next**
5. Apply weighted stats and thresholded native traits to a completed test tool.

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
- Grade, starcharged state, and calculated trait/stat data will be added to the
  same bounded payload before the first playable vertical slice.

## Current in-game validation

The alpha currently produces a diagnostic `Composite Alloy Sample`; it is not a
usable tool part yet. This deliberately tests the risky NBT-carrying smeltery
path before tool behavior is layered on top.

1. Install Silent Tinkers with the compatibility baseline mods listed above.
2. Make a `silentgear:alloy_ingot` through Silent Gear's normal alloy system.
3. Melt one ingot in a Tinkers smeltery or melter at 1200 C or hotter.
4. Pour one ingot (90 mB) into an empty casting table with no cast.
5. Hover the resulting sample. Its tooltip should list the canonical material
   IDs, percentages, and composition fingerprint.

Do not mix a second composition into a tank already holding composite alloy.
The intended behavior is for differently tagged fluid stacks to remain
separate; this still needs an in-game pack test against every supported tank and
pipe implementation.

## Automated build

Every push runs `./gradlew build` on Java 17 in GitHub Actions and uploads the
reobfuscated jar as a workflow artifact. A green build proves compilation,
resource processing, tests, and Forge reobfuscation; Minecraft behavior still
requires the short in-game validation above.
