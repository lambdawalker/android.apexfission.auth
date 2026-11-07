# Troubleshooting

Scope: Development/source. Keep [backend operations](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/agents/troubleshooting.md)
and Android/device evidence separate.

| Symptom | Diagnose | Correct response |
| --- | --- | --- |
| Gradle sync fails at generated assets | Compare current `auth/build.gradle.kts`; assets use AGP generated-source registration | Use the committed task/API; do not cast legacy Android source sets or assume a corrupt cache |
| Model/detector conflict | Compare isolated `cardModelAssets` with dependency catalog | Keep model extraction nontransitive; do not add two versions of the shared JitPack identity to runtime |
| Verification link opens browser | Check manifest host, package/certificate, public `assetlinks.json` and installed signing identity | Follow [signing](../android-signing.md); index discovery does not establish App Links |
| Link on another device needs a code | The local A proof is missing | Use B + C; never confirm with B alone or copy proof secrets between logs |
| Settings unavailable | Check index binding, `/v1/app-config`, cache expiry and validation | Retry trusted settings. Do not infer a default successful policy or silently change host |
| Stuck reading, state `finalizing` | Worker file validation and durable ready transition have not completed | Check S3, DynamoDB and the worker queue |
| `ready` with extraction `pending` | Extraction queued/running | Check the worker queue and capture status |
| Extraction `failed` | Service could not extract text | Show empty manual entry, not untrusted partial provider output |
| Save failed or stale revision | Compare returned `reviewed` values and draft revision | Preserve local edits; refresh/reconcile. Do not navigate to finish without matching server acknowledgement |
| Recapture needed after restart | Upload bytes were memory-only | Retake unfinished photos; IDs/checkpoints cannot recreate image bytes |
| Passkey sheet cancelled/unsupported | Catch `PasskeyCancelled` / `PasskeyUnsupported` | Offer supported retry/fallback; do not mark registration successful |
| Blank or missing screenshot | Check required scenario inventory, fixture state and renderer report | Render candidates and inspect; never silently replace reviewed baselines to make validation pass |

## Log safety

Document capture omits OCR debug logs. Use the capture state to verify the processing outcome.

Do not log token/password values, email-link secrets, JPEG bytes, signed URLs,
card text, provider response bodies or raw exceptions containing those values.
No current Android log demonstrates the deployed backend revision by itself.
