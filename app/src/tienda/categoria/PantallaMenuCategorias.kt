package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.components.LegalDocType
import com.example.ui.components.LegalDocumentDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaScreen(
    products: List<Product>,
    onCategorySelected: (String) -> Unit,
    onDeleteAccount: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var activeLegalDoc by remember { mutableStateOf<LegalDocType?>(null) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { CabeceraMenuCategorias(onBack = onBack) },
        containerColor = Color.White
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompact = maxWidth < 350.dp
            val isWide = maxWidth >= 600.dp
            val horizontalPad = when {
                isCompact -> 12.dp
                isWide -> 24.dp
                else -> 20.dp
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .testTag("menu_screen"),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 12.dp)
            ) {
                item {
                    Text(
                        text = "Selecciona una categoría para explorar prendas seleccionadas y accesorios:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray,
                            fontSize = if (isCompact) 13.sp else 14.sp
                        ),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                items(storeCategoriesList, key = { it.id }) { catItem ->
                    val count = if (catItem.id == "todo") {
                        products.size
                    } else {
                        products.count { it.category.equals(catItem.id, ignoreCase = true) }
                    }
                    TarjetaCategoriaMenu(
                        catItem = catItem,
                        productCount = count,
                        isCompact = isCompact,
                        onClick = { onCategorySelected(catItem.id) }
                    )
                }

                item {
                    SeccionLegalYCuentaMenu(
                        isCompact = isCompact,
                        onOpenLegalDoc = { activeLegalDoc = it },
                        onRequestDeleteAccount = { showDeleteAccountDialog = true }
                    )
                }
            }
        }
    }

    activeLegalDoc?.let { docType ->
        LegalDocumentDialog(
            docType = docType,
            onDismiss = { activeLegalDoc = null }
        )
    }

    if (showDeleteAccountDialog) {
        DialogoEliminarCuenta(
            onConfirm = {
                showDeleteAccountDialog = false
                onDeleteAccount()
            },
            onDismiss = { showDeleteAccountDialog = false }
        )
    }
}
