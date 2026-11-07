# Capture HTTP contract

The old `/identity/v1/submissions` draft is retired. Android implements `/onboarding/id/...` with direct S3 PUT, asynchronous finalization and status recovery.

The canonical implementation contract is [the Go card-capture implementation guide](https://raw.githubusercontent.com/lambdawalker/go.appexfission.aws.card-capture/main/docs/agents/index.md). Both app variants use the live API implementation. See [Android capture](../identity-capture.md) for use and limitations.

Review saves use `POST /onboarding/id/captures/{id}/review` with `expected_revision`, edited `data` and `origin_country`. Only the returned or reconciled matching `reviewed` object confirms persistence. See [save and finish](../identity-capture.md#save-and-finish).

Retakes use `POST /onboarding/id/captures/{id}/retake` with `operation_key`
and `expected_revision`. Match the finalized pair by `capture_id` and
`image_revision` before showing OCR after photo confirmation. Review data uses
`issued_identifier`; Android can also read legacy `personal_number` responses.
