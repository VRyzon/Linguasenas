package com.example.linguasenas.ui

import androidx.fragment.app.Fragment
import com.example.linguasenas.R
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.linguasenas.databinding.LoginViewFragmentBinding
import com.google.firebase.auth.FirebaseAuth


class LoginViewFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private var _binding:LoginViewFragmentBinding?=null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LoginViewFragmentBinding.inflate(inflater,container,false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.buttonLogin.setOnClickListener {
            loginUser()
        }

        binding.buttonGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginViewFragment_to_registerUserFragment2)
        }
    }

    private fun loginUser() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (TextUtils.isEmpty(email)) {
            binding.editTextEmail.error = "El correo electrónico es obligatorio"
            binding.editTextEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Ingresa un correo electrónico válido"
            binding.editTextEmail.requestFocus()
            return
        }

        if (TextUtils.isEmpty(password)) {
            binding.editTextPassword.error = "La contraseña es obligatoria"
            binding.editTextPassword.requestFocus()
            return
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_loginViewFragment_to_dashboardFragment22)
                } else {
                    val errorMessage = task.exception?.message
                    Toast.makeText(context, "Error en el inicio de sesión: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }
}
