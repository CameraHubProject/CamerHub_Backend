# Figma source of truth

The user explicitly selected **Figma for both interface and business behavior/API** in the current conversation. This supersedes the older 12-flow document wherever they conflict. Do not substitute controls, labels, business promises or flows based on developer preference or gaps in the existing schema.

Reference file: `UABy3sOnkzlVcGwtQbKAJn`, EXE CP3. Prototype starting point: `50:1583`; linked catalog: `44:897`. Design URL: https://www.figma.com/design/UABy3sOnkzlVcGwtQbKAJn/EXE-CP3?node-id=0-1

The previous frontend pass was a visual approximation and intentionally omitted/substituted multiple design features. That implementation is not the acceptance standard for this task. The current task requires a screen/control inventory, an API/data contract derived from observed Figma screens, and implementation plus verification of the actual prototype behaviors.

Confirmed reference differences to implement rather than remove: three-month warranty, 23-step inspection, phone/email account UI and OTP, showroom/address selection, remember-login control, purpose-based catalog filters, product condition/inspection scores/discounts, cart/purchase actions and AI/buyback navigation. Detailed rules must come from observing their corresponding prototype screens; do not infer policies that have not been shown.

Keep V1 and the original database-source repository intact. Add versioned migrations where the Figma-backed domain requires new data. Preserve existing application data and demo identities. Prototype demo values may be seeded as explicit synthetic fixtures, but live values must be backed by actual records. External SMS, AI, payments or shipping credentials/services must not be invented or reported as connected without verification.

The user has authorized the frontend and missing API work as one task; older phase-by-phase `next` gates do not apply to this scope. No new permission is needed for routine reversible implementation, API integration or tests. Secrets belong only in ignored server-side configuration.
