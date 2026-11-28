package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.model.AnnouncementDestination
import com.example.data.model.CategoryItem
import com.example.data.model.Product
import com.example.data.model.announcementDestination
import com.example.data.model.toAnnouncement
import com.example.data.model.toProduct
import com.example.ui.CatalogoScreen
import com.example.ui.ChatsScreen
import com.example.ui.InicioScreen
import com.example.ui.PerfilScreen
import com.example.ui.ProductoScreen
import com.example.ui.auth.VentanaEmergenteRegistro
import com.example.ui.theme.EleganTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasInteraccionInvitadoRegistroTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        instalarImageLoaderOfflineParaPruebas(obtenerContextoPrueba())
    }

    @After
    fun tearDown() {
        restaurarImageLoaderPruebas()
    }

    private fun crearProductoEjemplo(id: String = "prod-invitado-1"): Product = Product(
        id = id,
        name = "Camisa Sartorial Oxford",
        description = "Corte elegante de algodón peinado.",
        categoryId = "cat-camisas",
        price = 75.0,
        inStock = true,
        imageUrl = "https://cdn.example.com/camisa.jpg",
        images = listOf("https://cdn.example.com/camisa.jpg")
    )

    private fun crearCategoriasEjemplo(): List<CategoryItem> = listOf(
        CategoryItem(id = "cat-camisas", title = "Camisas")
    )

    @Test
    fun `guest tapping like or save on Inicio opens registration popup instead of failing and dismisses cleanly`() {
        var popupOpenCount = 0
        var lastPrompt = ""

        composeTestRule.setContent {
            EleganTheme {
                var showPopup by remember { mutableStateOf(false) }
                var promptReason by remember { mutableStateOf<String?>(null) }

                val onRequireAuth: (String, () -> Unit) -> Unit = { reason, _ ->
                    popupOpenCount++
                    lastPrompt = reason
                    promptReason = reason
                    showPopup = true
                }

                InicioScreen(
                    featuredProducts = listOf(crearProductoEjemplo("p1")),
                    categories = crearCategoriasEjemplo(),
                    publicationStats = emptyMap(),
                    likedPublicationIds = emptySet(),
                    publicationComments = emptyList(),
                    isAuthenticated = false,
                    onRequireAuth = onRequireAuth,
                    onToggleLike = { _, _ ->
                        onRequireAuth("Regístrate para dar me gusta a tus publicaciones favoritas.") {}
                    },
                    onOpenComments = {},
                    onSubmitComment = { _, _, _, _ -> },
                    onRecordShare = { _, _ -> },
                    onToggleSaveProduct = { _, _ ->
                        onRequireAuth("Regístrate para guardar publicaciones en tu perfil.") {}
                    },
                    onNavigateToCatalogo = {},
                    onNavigateToProduct = {},
                    onNavigateToMenu = {},
                    onNavigateToChats = {}
                )

                if (showPopup) {
                    VentanaEmergenteRegistro(
                        promptMessage = promptReason,
                        onDismiss = { showPopup = false },
                        onAuthSuccess = { showPopup = false }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("inicio_screen").performScrollToKey("p1")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_like_p1").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, popupOpenCount)
        assertTrue(lastPrompt.contains("Regístrate"))
        composeTestRule.onNodeWithTag("register_popup_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_email_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_password_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_confirm_password_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_accept_terms_checkbox").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Crear Mi Cuenta Elegan").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("register_popup_dismiss_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("register_popup_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithText("No se pudo actualizar el like").assertDoesNotExist()
    }

    @Test
    fun `guest tapping share does not open registration popup`() {
        var popupOpened = false
        var shareRecorded = false

        composeTestRule.setContent {
            EleganTheme {
                InicioScreen(
                    featuredProducts = listOf(crearProductoEjemplo("p-share")),
                    categories = crearCategoriasEjemplo(),
                    publicationStats = emptyMap(),
                    likedPublicationIds = emptySet(),
                    publicationComments = emptyList(),
                    isAuthenticated = false,
                    onRequireAuth = { _, _ -> popupOpened = true },
                    onToggleLike = { _, _ -> },
                    onOpenComments = {},
                    onSubmitComment = { _, _, _, _ -> },
                    onRecordShare = { _, onResult ->
                        shareRecorded = true
                        onResult(Result.success(Unit))
                    },
                    onNavigateToCatalogo = {},
                    onNavigateToProduct = {},
                    onNavigateToMenu = {},
                    onNavigateToChats = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("inicio_screen").performScrollToKey("p-share")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_share_p-share").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertFalse(popupOpened)
        assertFalse(shareRecorded) // Share opens Android chooser first; crucial check is popupOpened == false
        composeTestRule.onNodeWithTag("register_popup_dialog").assertDoesNotExist()
    }

    @Test
    fun `guest tapping save in Catalogo opens registration popup without showing false saved message`() {
        var saveToggledBeforeAuth = false

        composeTestRule.setContent {
            EleganTheme {
                var showPopup by remember { mutableStateOf(false) }

                CatalogoScreen(
                    products = listOf(crearProductoEjemplo("p-cat-1")),
                    categories = crearCategoriasEjemplo(),
                    selectedCategory = null,
                    onCategoryChange = {},
                    onNavigateToMenu = {},
                    onProductClick = {},
                    isAuthenticated = false,
                    onRequireAuth = { _, _ -> showPopup = true },
                    onToggleSaveProduct = { saveToggledBeforeAuth = true }
                )

                if (showPopup) {
                    VentanaEmergenteRegistro(
                        promptMessage = "Regístrate para guardar prendas en tu colección.",
                        onDismiss = { showPopup = false },
                        onAuthSuccess = { showPopup = false }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("catalogo_screen").performScrollToNode(hasTestTag("catalog_save_p-cat-1"))
        composeTestRule.onNodeWithTag("catalog_save_p-cat-1").performClick()
        composeTestRule.waitForIdle()

        assertFalse(saveToggledBeforeAuth)
        composeTestRule.onNodeWithTag("register_popup_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guardado en tu colección").assertDoesNotExist()
    }

    @Test
    fun `guest tapping register action or saving delivery on Perfil opens registration popup`() {
        composeTestRule.setContent {
            EleganTheme {
                var showPopup by remember { mutableStateOf(false) }
                var promptReason by remember { mutableStateOf<String?>(null) }

                PerfilScreen(
                    userProfile = null,
                    orders = emptyList(),
                    isAuthenticated = false,
                    onRequireAuth = { reason, _ ->
                        promptReason = reason
                        showPopup = true
                    },
                    onSaveProfile = { _, _, _, _, _, _ -> },
                    onNavigateToChats = {},
                    onSignOut = {}
                )

                if (showPopup) {
                    VentanaEmergenteRegistro(
                        promptMessage = promptReason,
                        onDismiss = { showPopup = false },
                        onAuthSuccess = { showPopup = false }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Visitante Elegan").assertIsDisplayed()
        composeTestRule.onNodeWithTag("perfil_screen").performScrollToNode(hasTestTag("guest_register_button"))
        composeTestRule.onNodeWithTag("guest_register_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("register_popup_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("register_popup_close_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("register_popup_dialog").assertDoesNotExist()

        composeTestRule.onNodeWithTag("perfil_screen").performScrollToNode(hasTestTag("profile_delivery_menu"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_delivery_menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("perfil_screen").performScrollToNode(hasTestTag("guest_register_button"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("save_profile_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("register_popup_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("El nombre y el correo son obligatorios.").assertDoesNotExist()
    }

    @Test
    fun `guest sending message in Chats opens registration popup and preserves draft`() {
        var sentMessageDirectly = false

        composeTestRule.setContent {
            EleganTheme {
                var showPopup by remember { mutableStateOf(false) }

                ChatsScreen(
                    messages = emptyList(),
                    isAuthenticated = false,
                    onRequireAuth = { _, _ -> showPopup = true },
                    onSendMessage = { _, _, _ -> sentMessageDirectly = true }
                )

                if (showPopup) {
                    VentanaEmergenteRegistro(
                        promptMessage = "Regístrate para chatear con Elegan Concierge.",
                        onDismiss = { showPopup = false },
                        onAuthSuccess = { showPopup = false }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_input_field").performTextInput("Hola Concierge")
        composeTestRule.onNodeWithTag("chat_send_button").performClick()
        composeTestRule.waitForIdle()

        assertFalse(sentMessageDirectly)
        composeTestRule.onNodeWithTag("register_popup_dialog").assertIsDisplayed()

        composeTestRule.onNodeWithTag("register_popup_dismiss_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("register_popup_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithText("Hola Concierge").assertIsDisplayed()
    }

    @Test
    fun `inicio feed video does not autoplay and starts only when user taps play button`() {
        val videoUrl = "https://cdn.example.com/video.mp4"

        val videoProduct: Product = mapOf(
            "id" to "p-video-1",
            "name" to "Conjunto Punto",
            "description" to "Video de pasarela",
            "category" to "cat-camisas",
            "price" to 50.0,
            "inStock" to true,
            "hasVideo" to true,
            "videoUrl" to videoUrl,
            "imageUrl" to "https://cdn.example.com/poster.jpg"
        ).toProduct()

        composeTestRule.setContent {
            EleganTheme {
                InicioScreen(
                    featuredProducts = listOf(videoProduct),
                    categories = crearCategoriasEjemplo(),
                    publicationStats = emptyMap(),
                    likedPublicationIds = emptySet(),
                    publicationComments = emptyList(),
                    onToggleLike = { _, _ -> },
                    onOpenComments = {},
                    onSubmitComment = { _, _, _, _ -> },
                    onRecordShare = { _, _ -> },
                    onNavigateToCatalogo = {},
                    onNavigateToProduct = {},
                    onNavigateToMenu = {},
                    onNavigateToChats = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("inicio_screen").performScrollToKey("p-video-1")
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400L)
        composeTestRule.waitForIdle()

        // El video no debe reproducirse automáticamente: el botón de Play debe permanecer visible.
        composeTestRule.onNodeWithTag("inicio_video_p-video-1_0_play_btn").performScrollTo().assertIsDisplayed()

        // El usuario decide reproducirlo tocando el botón de Play.
        // Desactivamos autoAdvance para evitar que el CircularProgressIndicator de carga bloquee waitForIdle.
        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.onNodeWithTag("inicio_video_p-video-1_0_play_btn").performClick()
            composeTestRule.mainClock.advanceTimeBy(500L)
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.onNodeWithTag("inicio_video_p-video-1_0_play_btn").assertDoesNotExist()
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun `producto detail video does not autoplay and starts only when user clicks play`() {
        val videoUrl = "https://cdn.example.com/producto-detail.mp4"

        val videoProduct: Product = mapOf(
            "id" to "p-detail-video",
            "name" to "Conjunto Beige",
            "description" to "Detalle del producto",
            "category" to "cat-camisas",
            "price" to 50.0,
            "inStock" to true,
            "hasVideo" to true,
            "videoUrl" to videoUrl,
            "imageUrl" to "https://cdn.example.com/poster-detail.jpg"
        ).toProduct()

        composeTestRule.setContent {
            EleganTheme {
                ProductoScreen(
                    product = videoProduct,
                    cartItemCount = 0L,
                    categories = crearCategoriasEjemplo(),
                    onAddToCart = { _, _, _, _, _ -> },
                    onBuyNow = { _, _, _, _, _ -> },
                    onNavigateToCart = {},
                    onBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400L)
        composeTestRule.waitForIdle()

        // El video en detalle de producto no debe reproducirse automáticamente.
        composeTestRule.onNodeWithTag("gallery_video_p-detail-video_play_btn").assertIsDisplayed()

        // Solo se reproduce cuando el usuario hace clic en él.
        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.onNodeWithTag("gallery_video_p-detail-video_play_btn").performClick()
            composeTestRule.mainClock.advanceTimeBy(500L)
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.onNodeWithTag("gallery_video_p-detail-video_play_btn").assertDoesNotExist()
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun `inicio announcement cards navigate to matching category and product destinations`() {
        val categoryUuid = "c8d8cbe6-afab-4cff-a036-9a404c461e25"
        val productUuid = "37eef6ac-148d-4024-b5ec-92a39ebdcd66"
        var navigatedCategory: String? = null
        var navigatedProductId: String? = null

        val announcements = listOf(
            mapOf(
                "id" to "ann-1",
                "title" to "Promo Camizas",
                "message" to "Ver colección",
                "badge" to "DESCUENTO",
                "destinationType" to "category",
                "destinationId" to categoryUuid
            ).toAnnouncement(),
            mapOf(
                "id" to "ann-2",
                "title" to "oferta",
                "message" to "Ir directo al producto",
                "badge" to "CUPON",
                "destinationType" to "product",
                "destinationId" to productUuid
            ).toAnnouncement()
        )

        composeTestRule.setContent {
            EleganTheme {
                InicioScreen(
                    featuredProducts = listOf(crearProductoEjemplo(productUuid)),
                    categories = listOf(CategoryItem(id = categoryUuid, title = "Camiza")),
                    announcements = announcements,
                    publicationStats = emptyMap(),
                    likedPublicationIds = emptySet(),
                    publicationComments = emptyList(),
                    onToggleLike = { _, _ -> },
                    onOpenComments = {},
                    onSubmitComment = { _, _, _, _ -> },
                    onRecordShare = { _, _ -> },
                    onNavigateToCatalogo = { navigatedCategory = it },
                    onAnnouncementClick = { announcement ->
                        when (val destination = announcement.announcementDestination()) {
                            is AnnouncementDestination.Category -> navigatedCategory = destination.categoryId
                            is AnnouncementDestination.Product -> navigatedProductId = destination.productId
                            AnnouncementDestination.None -> navigatedCategory = null
                        }
                    },
                    onNavigateToProduct = {},
                    onNavigateToMenu = {},
                    onNavigateToChats = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("inicio_screen").performScrollToKey("announcements_section")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("announcement_card_0").performClick()
        composeTestRule.waitForIdle()
        assertEquals(categoryUuid, navigatedCategory)

        composeTestRule.onNodeWithTag("inicio_announcements_row").performScrollToKey("ann-2")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("announcement_card_1").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(productUuid, navigatedProductId)
    }
}
