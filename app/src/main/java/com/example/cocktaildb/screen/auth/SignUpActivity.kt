package com.example.cocktaildb.screen.auth

import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.cocktaildb.MainActivity
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.databinding.ActivitySignUpBinding
import com.example.cocktaildb.utils.base.BaseActivity

class SignUpActivity : BaseActivity<ActivitySignUpBinding>(), AuthContract.View {

    private lateinit var presenter: AuthPresenter

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                presenter.handleGoogleSignInResult(result.data)
            } else {
                hideLoading()
                showMessage(getString(R.string.msg_google_signin_failed))
            }
        }

    override fun inflateViewBinding() = ActivitySignUpBinding.inflate(layoutInflater)

    override fun initView() {
        supportActionBar?.hide()

        viewBinding.btnSignUp.setOnClickListener {
            val name = viewBinding.etName.text.toString().trim()
            val email = viewBinding.etEmail.text.toString().trim()
            val password = viewBinding.etPassword.text.toString().trim()
            val confirmPassword = viewBinding.etConfirmPassword.text.toString().trim()
            val termsAccepted = viewBinding.cbTerms.isChecked

            presenter.signUpWithEmail(name, email, password, confirmPassword, termsAccepted)
        }

        viewBinding.btnGoogle.setOnClickListener {
            presenter.loginWithGoogle()
        }

        viewBinding.tvLoginLink.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        val authRepository = AuthRepository(this)
        presenter = AuthPresenter(authRepository).apply {
            setView(this@SignUpActivity)
            onStart()
        }
    }

    override fun onLoginSuccess() {
        showMessage(getString(R.string.Welcome))
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onLoginFailure(error: String) {
        showMessage(error)
    }

    override fun showLoading() {
        // TODO: Show loading dialog or progress bar
    }

    override fun hideLoading() {
        // TODO: Hide loading dialog or progress bar
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun launchGoogleSignIn(intent: Intent) {
        googleSignInLauncher.launch(intent)
    }

    override fun onStop() {
        presenter.onStop()
        super.onStop()
    }
}
