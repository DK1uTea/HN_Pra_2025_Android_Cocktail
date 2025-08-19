package com.example.cocktaildb

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.example.cocktaildb.databinding.ActivityStartBinding
import com.example.cocktaildb.screen.auth.SignInActivity
import android.view.animation.AnimationUtils
import com.example.cocktaildb.data.repository.AuthRepository

import com.google.firebase.auth.FirebaseAuth
class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If already authenticated (Firebase persists session), skip auth screens
        val authRepository = AuthRepository(this)
        if (authRepository.isUserLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            val name = FirebaseAuth.getInstance().currentUser?.displayName
            if (!name.isNullOrBlank()) {
                Toast.makeText(this, getString(R.string.Welcome_back) + ", " + name, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.Welcome_back), Toast.LENGTH_SHORT).show()
            }
            finish()
            return
        }
        // Theme already hides action bar

        // Start animations after view is laid out
        binding.root.post {
            startEntranceAnimations()
        }

        // Set up click listener for start button
        binding.btnStart.setOnClickListener {
            // Navigate to login screen with animation
            val intent = Intent(this, SignInActivity::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            startActivity(intent, options.toBundle())
            finish() // Close StartActivity so user can't go back
        }
    }

    private fun startEntranceAnimations() {
        // Animate title with scale and bounce effect
        binding.tvTitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in))

        // Animate subtitle with fade in
        binding.tvSubtitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))

        // Animate start button with slide up and scale
        binding.btnStart.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))

        // Add staggered delay for better visual effect
        binding.tvSubtitle.alpha = 0f
        binding.btnStart.alpha = 0f

        binding.tvSubtitle.animate()
            .alpha(1f)
            .setStartDelay(300)
            .setDuration(800)
            .start()

        binding.btnStart.animate()
            .alpha(1f)
            .setStartDelay(600)
            .setDuration(800)
            .start()
    }
}

