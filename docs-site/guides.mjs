// The published, user-facing docs (allowlist). Shared by the sync script and the
// VitePress config so the set and its order are defined exactly once.
//   file  — source markdown, relative to the REPOSITORY ROOT (so module readmes
//           like the extender's can be published alongside docs/)
//   title — sidebar / nav label
//   group — sidebar section the entry belongs to
//   slug  — optional route name override; defaults to the file's base name
//
// Internal dev docs (agent prompts, the German delegate-registry analysis,
// ci.md, ip-dash notes, root-level working documents) are deliberately NOT
// listed here and stay unpublished (browsed on GitHub).
export const GUIDES = [
  { file: 'docs/code-generation-guide.md', title: 'Code Generation', group: 'User Manual' },
  { file: 'docs/configuration-guide.md', title: 'Configuration Guide', group: 'User Manual' },
  {
    file: 'org.eclipse.fennec.emf.osgi.extender/readme.md',
    title: 'Model Extender',
    group: 'User Manual',
    slug: 'model-extender',
  },
  { file: 'docs/emf-delegate-user-guide.md', title: 'EMF Delegates', group: 'User Manual' },
  {
    file: 'docs/model-fingerprint-guide.md',
    title: 'Model Fingerprints',
    group: 'User Manual',
    slug: 'model-fingerprints',
  },
  {
    file: 'docs/metadata-service-guide.md',
    title: 'Metadata Service',
    group: 'User Manual',
    slug: 'metadata-service',
  },
  {
    file: 'docs/eobject-registry-guide.md',
    title: 'EObject Registries',
    group: 'User Manual',
    slug: 'eobject-registries',
  },
];

// Route name for a guide: an explicit `slug`, otherwise the file's base name
// without the .md extension, e.g. 'docs/configuration-guide.md' ->
// 'configuration-guide', served at /guides/configuration-guide.
export function slugFor(guide) {
  if (guide.slug) return guide.slug;
  return guide.file.replace(/^.*\//, '').replace(/\.md$/, '');
}
