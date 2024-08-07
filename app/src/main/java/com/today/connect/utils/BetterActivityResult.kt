package com.today.connect.utils

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import com.today.connect.activity.MainActivity


class BetterActivityResult<Input, ActivityResult : Any> private constructor(
    caller: ActivityResultCaller,
    contract: ActivityResultContract<Input, ActivityResult>,
    private var onActivityResult: ((activityResult: ActivityResult) -> Unit)?
) {
    private val launcher: ActivityResultLauncher<Input>
    /**
     * Launch activity, same as [ActivityResultLauncher.launch] except that it allows a callback
     * executed after receiving a result from the target activity.
     */
    /**
     * Same as [.launch] with last parameter set to `null`.
     */
    @JvmOverloads
    fun launch(
        input: Input,
        onActivityResult: ((ActivityResult) -> Unit)? = this.onActivityResult
    ) {
        if (onActivityResult != null) {
            this.onActivityResult = onActivityResult
        }
        launcher.launch(input)
    }

    private fun callOnActivityResult(result: ActivityResult) {
        if (onActivityResult != null) onActivityResult?.let { it(result) }
    }

    companion object {
        /**
         * Register activity result using a [ActivityResultContract] and an in-place activity result callback like
         * the default approach. You can still customise callback using [.launch].
         */
        fun <Input, ActivityResult : Any> registerForActivityResult(
            caller: ActivityResultCaller,
            contract: ActivityResultContract<Input, ActivityResult>,
            @Nullable onActivityResult: ((activityResult: Any) -> Unit)?
        ): BetterActivityResult<Input, ActivityResult> {
            return BetterActivityResult(caller, contract, onActivityResult)
        }

        /**
         * Same as [.registerForActivityResult] except
         * the last argument is set to `null`.
         */
        fun registerForActivityResult(
            caller: ActivityResultCaller,
            contract: ActivityResultContracts.StartActivityForResult
        ): BetterActivityResult<Intent, ActivityResult> {
            return registerForActivityResult(caller, contract, null)
        }

        /**
         * Specialised method for launching new activities.
         */
        fun registerActivityForResult(
            caller: MainActivity
        ): BetterActivityResult<Intent, ActivityResult> {
            return registerForActivityResult(caller,
                ActivityResultContracts.StartActivityForResult()
            )
        }
    }

    init {
        launcher = caller.registerForActivityResult(
            contract
        ) { result: ActivityResult -> callOnActivityResult(result) }
    }
}