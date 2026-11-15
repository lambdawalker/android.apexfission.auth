package com.apexfission.android.auth.discovery

import com.apexfission.android.auth.passkey.AccountStatus
import org.junit.Test

class RequiredOnlyPolicyTest {
    @Test fun optionalSetupNeverBlocksServerConfirmedCompletion() {
        val policy = OnboardingPolicy(Requirement.OPTIONAL, Requirement.OPTIONAL, Requirement.OPTIONAL,
            PasswordPlacement.AFTER_VERIFICATION, PasswordPolicy())
        check(policy.nextRequired(AccountStatus()) == OnboardingStep.REFRESH)
        check(policy.nextRequired(AccountStatus(onboardingComplete = true)) == OnboardingStep.COMPLETE)
        check(policy.optionalAccountSteps(AccountStatus(onboardingComplete = true)) == setOf(OnboardingStep.PASSWORD, OnboardingStep.PASSKEY, OnboardingStep.ID_CAPTURE))
        check(policy.optionalAccountSteps(AccountStatus()).isEmpty())
        // Existing consumers can still explicitly choose the legacy optional-offer sequence.
        check(policy.next(AccountStatus(onboardingComplete = true)) == OnboardingStep.PASSWORD)
    }
    @Test fun allPolicyCombinationsKeepRequiredStepsAndServerCompletionAuthoritative() {
        for (password in Requirement.values()) for (passkey in Requirement.values()) for (capture in Requirement.values()) {
            val policy = OnboardingPolicy(capture, passkey, password,
                if (password == Requirement.DISABLED) PasswordPlacement.HIDDEN else PasswordPlacement.AFTER_VERIFICATION, PasswordPolicy())
            val expected = when {
                password == Requirement.REQUIRED -> OnboardingStep.PASSWORD
                passkey == Requirement.REQUIRED -> OnboardingStep.PASSKEY
                capture == Requirement.REQUIRED -> OnboardingStep.ID_CAPTURE
                else -> OnboardingStep.COMPLETE
            }
            check(policy.nextRequired(AccountStatus(onboardingComplete = true)) == expected)
            check(policy.nextRequired(AccountStatus(true, true, true, false)) == OnboardingStep.REFRESH)
            check(policy.nextRequired(AccountStatus(true, true, true, true)) == OnboardingStep.COMPLETE)
            check(policy.optionalAccountSteps(AccountStatus(true, true, true, true)).isEmpty())
        }
    }
}
