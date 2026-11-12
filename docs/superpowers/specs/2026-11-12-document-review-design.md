# Document review

Approved in conversation: grouped, themed personal/document information cards; document-type dropdown (national ID, passport, driver's license, residence permit, voter ID, Another document with custom name); relevant primary fields and expandable additional details without discarding values. Unknown document types retain their name and use generic fields.

Issuing jurisdiction is separate from holder origin/residence. One Issued in row opens a bottom sheet with searchable country and optional region/city selectors supporting arbitrary manual entry. Changing a parent clears descendants within the temporary sheet draft; explanatory copy makes this explicit. Apply commits the three values together; dismiss cancels. Country-only is valid. OCR must leave uncertain values blank and never invent the hierarchy. Issuing authority is separate. Preserve consent, capture type, upload flow and completion semantics.

Use existing Compose/Material theme and pinned toolchain. Do not retain document images longer or invent thumbnails when private images are no longer available. Coordinated feature/Android/host PRs, no deployment or publication.
