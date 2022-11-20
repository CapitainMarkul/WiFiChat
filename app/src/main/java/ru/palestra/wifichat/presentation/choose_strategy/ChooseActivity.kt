package ru.palestra.wifichat.presentation.choose_strategy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.palestra.wifichat.presentation.client.ClientChatActivity
import ru.palestra.wifichat.presentation.server_host.ServerActivity
import ru.palestra.wifichat.databinding.ActivityChooseBinding

class ChooseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChooseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChooseBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        binding.txtClientBtn.setOnClickListener {
            startActivity(ClientChatActivity.createIntent(this))
        }

        binding.txtServerBtn.setOnClickListener {
            startActivity(ServerActivity.createIntent(this))
        }
    }
}