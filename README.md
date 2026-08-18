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

1. Read a two-material Silent Gear alloy ingot.
2. Convert its composition into a canonical, versioned representation.
3. Preserve that representation through one controlled casting path.
4. Create one Tinkers tool part carrying the representation.
5. Apply weighted stats to a completed test tool.

The initial code establishes step 2 without registering every possible alloy as
a global material. Equivalent ratios share the same compact fingerprint.
