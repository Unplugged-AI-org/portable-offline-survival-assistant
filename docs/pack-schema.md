# Pack Schema

Content packs are the foundation for guide cards, provenance, search, and future retrieval.

## Pack Principles

- Packs are local-first.
- Every pack must include explicit provenance.
- Every pack must include license metadata.
- Unreviewed packs must be labeled.
- Official, community, and user-imported packs must be visually distinct.
- Packs should be small enough for mobile use.

## Initial Pack Layout

```text
packs/
  wilderness-basics/
    manifest.json
    cards/
      water.md
      fire.md
      shelter.md
      navigation.md
      signaling.md
      battery.md
```

## Manifest Draft

```json
{
  "id": "wilderness-basics",
  "title": "Wilderness Basics",
  "version": "0.1.0",
  "category": "wilderness",
  "description": "Basic offline wilderness survival guidance.",
  "pack_type": "official",
  "license": "NOASSERTION",
  "source_name": "Draft placeholder - source not selected",
  "source_url": "",
  "author": "Unplugged AI",
  "last_reviewed": "2026-06-30",
  "review_status": "draft",
  "files": [
    "cards/water.md",
    "cards/fire.md",
    "cards/shelter.md",
    "cards/navigation.md",
    "cards/signaling.md",
    "cards/battery.md"
  ]
}
```

## Guide Card Metadata

Each guide card should support:

- Title.
- Category.
- Summary.
- Steps.
- Warnings.
- Source title.
- Source URL or publication reference.
- License.
- Pack ID.
- Pack version.
- Review status.
- Last reviewed date.

## Review Status Values

- `draft`
- `unreviewed`
- `community-reviewed`
- `expert-reviewed`
- `deprecated`

## Pack Type Values

- `official`
- `community`
- `user-imported`

v0 should include only bundled official draft content unless safe, licensed source material is available. Release packs should not use `NOASSERTION`; they need an explicit license.
