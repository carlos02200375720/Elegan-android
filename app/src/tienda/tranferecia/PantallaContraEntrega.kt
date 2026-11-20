package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EleganBurgundy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaContraEntrega(total: Double, isSubmitting: Boolean, onBack: () -> Unit, onAccept: () -> Unit) {
    BackHandler { onBack() }
    var accepted by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Pago contra entrega") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Antes de confirmar tu pedido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF6F1)), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Elegiste pagar cuando recibas tu pedido.", fontWeight = FontWeight.SemiBold)
                    Text("Preparar y enviar tu compra requiere coordinación, embalaje y un servicio de entrega hasta tu domicilio. Por eso, te pedimos que confirmes que estarás disponible para recibirla y pagar el monto indicado.")
                    Text("Total a pagar al recibir: RD$ ${"%.2f".format(total)}", fontWeight = FontWeight.Bold)
                    Text("Al aceptar, autorizas a Elegan a preparar y despachar el pedido. Si no puedes recibirlo, comunícate con nosotros lo antes posible.")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                Text("He leído y acepto estas condiciones.")
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onAccept, enabled = accepted && !isSubmitting, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy)) {
                Text(if (isSubmitting) "Registrando pedido..." else "Aceptar y confirmar pedido", fontWeight = FontWeight.Bold)
            }
        }
    }
}
