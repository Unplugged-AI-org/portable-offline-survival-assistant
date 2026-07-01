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

## Manifest v0

```json
{
  "id": "wilderness-basics",
  "title": "Wilderness Basics",
  "version": "0.1.0",
  "category": "wilderness",
  "description": "Starter offline guide cards for conservative wilderness preparation.",
  "pack_type": "official",
  "license": "U.S. federal government source synthesis; verify third-party notices on source pages",
  "source_name": "National Park Service; USDA Forest Service",
  "source_url": "https://www.nps.gov/articles/10essentials.htm",
  "author": "Unplugged AI",
  "last_reviewed": "2026-07-01",
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

## Markdown Card Front Matter

Guide cards are Markdown files with required YAML-style front matter followed by body Markdown:

```markdown
---
id: water
title: Water Planning
category: water
summary: Plan water before walking and carry a treatment method you already know how to use.
warnings: Natural water can contain hazards that are not visible.
source_title: National Park Service - Ten Essentials
source_url: https://www.nps.gov/articles/10essentials.htm
citation: National Park Service. Ten Essentials. Last updated May 28, 2026.
license: U.S. government work; public domain unless otherwise noted by the source
review_status: draft
reviewed_by: Unplugged AI content draft
reviewed_at: 2026-07-01
notes: POSA draft synthesis. Not medical advice.
sort_order: 10
---
## Field Use

- Identify likely water sources before travel and confirm whether they are seasonal.
```

The app stores card IDs as `pack_id:card_id` so separate packs can use short local card IDs without colliding.

## Guide Card Metadata

Each guide card should support:

- Title.
- Category.
- Summary.
- Body Markdown.
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
