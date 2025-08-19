package com.example.cocktaildb.screen.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.cocktaildb.MainActivity
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.databinding.ActivityLoginBinding
import com.example.cocktaildb.utils.base.BaseActivity

class SignInActivity : BaseActivity<ActivityLoginBinding>(), AuthContract.View {

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

    override fun inflateViewBinding(): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun initView() {
        supportActionBar?.hide()
        setupClickListeners()
    }

    override fun initData() {
        initPresenter()
        presenter.onStart()
    }

    private fun initPresenter() {
        val authRepository = AuthRepository(this)
        presenter = AuthPresenter(authRepository)
        presenter.setView(this)
    }

    private fun setupClickListeners() {
        viewBinding.btnSignIn.setOnClickListener {
            val email = viewBinding.etEmail.text.toString().trim()
            val password = viewBinding.etPassword.text.toString().trim()
            presenter.loginWithEmail(email, password)
        }

        viewBinding.tvForgotPassword.setOnClickListener {
            val email = viewBinding.etEmail.text.toString().trim()
            presenter.forgotPassword(email)
        }

        viewBinding.btnGoogle.setOnClickListener {
            presenter.loginWithGoogle()
        }

        viewBinding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onLoginSuccess() {
        showMessage(getString(R.string.Welcome_back))
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
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
