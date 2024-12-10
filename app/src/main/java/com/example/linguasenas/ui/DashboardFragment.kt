package com.example.linguasenas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.linguasenas.R
import com.example.linguasenas.databinding.DashboardFragmentBinding
import com.google.firebase.auth.FirebaseAuth

class DashboardFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private var _binding: DashboardFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DashboardFragmentBinding.inflate(inflater, container, false)

        // Configuración de botones
        binding.buttonsigncreate.setOnClickListener {
            // Navega al fragmento OnCameraFragment
            findNavController().navigate(R.id.action_dashboardFragment2_to_onCameraFragment)
        }

        binding.buttonGoToRegisterSign.setOnClickListener {
            // Navega al fragmento RegisterSignFragment
            findNavController().navigate(R.id.action_dashboardFragment2_to_registerSignFragment)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}