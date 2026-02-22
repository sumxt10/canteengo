# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in CanteenGo, please report it responsibly.

## Sensitive Files

The following files contain sensitive information and should **NEVER** be committed to the repository:

| File | Contains |
|------|----------|
| `google-services.json` | Firebase API keys, project IDs, OAuth client IDs |
| `local.properties` | Local SDK paths |
| `*.jks` / `*.keystore` | App signing keys |
| `keystore.properties` | Keystore passwords |
| `.env` files | Environment variables |

## For Contributors

1. **Never commit sensitive files** - Check `.gitignore` before committing
2. **Use environment variables** for any API keys in production
3. **Review Firebase Security Rules** before deploying
4. **Rotate credentials** if they are accidentally exposed

## Firebase Security

Ensure your Firestore Security Rules are properly configured:

- Users can only read/write their own data
- Menu items are read-public but write-restricted to admins
- Orders are protected and role-based

## If Credentials Are Exposed

If you accidentally commit sensitive data:

1. **Immediately revoke the exposed credentials**
2. **Generate new credentials** in Firebase Console
3. **Remove from git history** using `git filter-branch` or BFG Repo-Cleaner
4. **Force push** the cleaned history
5. **Notify** any affected parties

