package com.example.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.di.ContenedorDependenciasElegan
import com.example.data.di.obtenerContenedorElegan
import com.example.data.repository.EleganRepository

/**
 * Fábrica tipada de ViewModels que realiza inyección por constructor
 * compartiendo el mismo [EleganRepository] provisto por [ContenedorDependenciasElegan].
 */
class FabricaViewModelsElegan(
    private val repository: EleganRepository,
    private val autoLoad: Boolean = true
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(CatalogoViewModel::class.java) ->
            CatalogoViewModel(repository = repository, autoLoad = autoLoad) as T
        modelClass.isAssignableFrom(InteraccionesViewModel::class.java) ->
            InteraccionesViewModel(repository = repository, autoSync = autoLoad) as T
        modelClass.isAssignableFrom(CarritoYPedidosViewModel::class.java) ->
            CarritoYPedidosViewModel(repository = repository, autoLoad = autoLoad) as T
        modelClass.isAssignableFrom(PerfilYSoporteViewModel::class.java) ->
            PerfilYSoporteViewModel(repository = repository, autoLoad = autoLoad) as T
        else -> throw IllegalArgumentException("ViewModel desconocido para FabricaViewModelsElegan: ${modelClass.name}")
    }
}

@Composable
fun rememberEleganViewModelFactory(
    contenedor: ContenedorDependenciasElegan = LocalContext.current.obtenerContenedorElegan()
): ViewModelProvider.Factory = remember(contenedor) {
    FabricaViewModelsElegan(repository = contenedor.eleganRepository)
}
