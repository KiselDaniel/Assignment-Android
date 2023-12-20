package com.goodrequest.hiring.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.goodrequest.hiring.R
import com.goodrequest.hiring.model.Pokemon
import com.goodrequest.hiring.viewmodel.PokemonViewModel
import com.goodrequest.hiring.viewmodel.ResourceState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PokemonHomeScreen(
    pokemonViewModel: PokemonViewModel = hiltViewModel()
) {
    val pokemonViewState by pokemonViewModel.pokemonViewState.collectAsState()
    val lazyListState = rememberLazyListState()

    // handles paging logic when we scroll to the bottom of the list
    LaunchedEffect(lazyListState, pokemonViewState.pokemonList.size) {
        snapshotFlow { lazyListState.layoutInfo.totalItemsCount }
            .collect {
                if (shouldLoadMore(lazyListState, pokemonViewState.pokemonList)) {
                    pokemonViewModel.loadMorePokemon()
                }
            }
    }

    // handles pull to refresh logic
    val pullRefreshState = rememberPullRefreshState(
        refreshing = false,
        onRefresh = { pokemonViewModel.refreshPokemon() }
    )

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {

            when (val viewState = pokemonViewState.resourceState) {
                is ResourceState.Loading -> {
                    Loader()
                }

                is ResourceState.Refreshing -> {
                    if (pokemonViewState.pokemonList.isNotEmpty()) {
                        PokemonList(
                            pokemonList = pokemonViewState.pokemonList,
                            lazyListState = lazyListState
                        )
                    }
                    Loader()
                }

                is ResourceState.Success -> {
                    if (pokemonViewState.pokemonList.isNotEmpty()) {
                        PokemonList(
                            pokemonList = pokemonViewState.pokemonList,
                            lazyListState = lazyListState
                        )
                    }
                }

                is ResourceState.Error -> {
                    if (pokemonViewState.pokemonList.isNotEmpty()) {
                        PokemonList(
                            pokemonList = pokemonViewState.pokemonList,
                            lazyListState = lazyListState,
                            pageLoadingFailed = true,
                            onRetry = { pokemonViewModel.retryPage() }
                        )
                    } else {
                        RetryButton(onclick = { pokemonViewModel.retry() })
                    }
                    ErrorSnackbar(errorMessage = viewState.error)
                }
            }

            PullRefreshIndicator(
                refreshing = pokemonViewState.resourceState is ResourceState.Refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun PokemonItem(pokemon: Pokemon) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        PokemonNameTextComponent(textValue = pokemon.name)
        PokemonImageComponent(imageUrl = pokemon.detail?.image ?: "")
        Spacer(modifier = Modifier.size(16.dp))
        Row {
            if (pokemon.detail?.weight != null) {
                PokemonAttributeTextComponent(textValue = "Weight: ${pokemon.detail.weight} Kg")
            }
            Spacer(Modifier.weight(1f))
            if (pokemon.detail?.move != null) {
                PokemonAttributeTextComponent(textValue = "Move: ${pokemon.detail.move}")
            }
        }
        Divider()
    }
}

@Composable
private fun PokemonList(
    pokemonList: List<Pokemon>,
    lazyListState: LazyListState,
    pageLoadingFailed: Boolean = false,
    onRetry: () -> Unit = {}
) {
    LazyColumn (state = lazyListState) {
        items(pokemonList, key = { pokemon -> pokemon.id }) { pokemon ->
            PokemonItem(pokemon)
        }
        // show a loader at the end of the list if we are loading more items from next page
        if (shouldLoadMore(lazyListState, pokemonList) && !pageLoadingFailed) {
            item {
                Loader()
            }
        }
        // else show a retry button if we failed to load more items
        else if (pageLoadingFailed) {
            item {
                RetryButton(onRetry)
            }
        }
    }
}

@Composable
private fun PokemonNameTextComponent(textValue: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp),
        text = textValue,
        style = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = Color.DarkGray
        )
    )
}

@Composable
private fun PokemonAttributeTextComponent(textValue: String) {
    Text(
        modifier = Modifier
            .wrapContentHeight()
            .padding(16.dp),
        text = textValue,
        style = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            color = Color.DarkGray
        )
    )
}

@Composable
private fun PokemonImageComponent(imageUrl: String) {
    AsyncImage(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        model = imageUrl,
        contentDescription = "Pokemon Image",
        contentScale = ContentScale.Fit,
        placeholder = rememberAsyncImagePainter(R.drawable.placeholder_pokemon_image),
        error = rememberAsyncImagePainter(R.drawable.placeholder_pokemon_image),
    )
}

@Composable
fun Loader() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorSnackbar(
    errorMessage: String,
    duration: Long = 3000L,
) {
    var showSnackbar by remember { mutableStateOf(true) }

    if (showSnackbar) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            content = { Text(text = errorMessage) }
        )

        // LaunchedEffect to hide the snackbar after a delay
        LaunchedEffect(key1 = Unit) {
            delay(duration)
            showSnackbar = false
        }
    }
}

@Composable
fun RetryButton(onclick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = { onclick() }) {
            Text(text = "Retry")
        }
    }
}

/**
 * Determines whether more Pokemon should be loaded based on the current scroll position.
 *
 * @param lazyListState The state object used for observing the list's scroll state.
 * @param pokemonList The current list of displayed Pokemon.
 * @param bufferItemCount The number of items before the end of the list to start loading more.
 * @return `true` if the user has scrolled to within [bufferItemCount] items from the end of the list, `false` otherwise.
 */
fun shouldLoadMore(
    lazyListState: LazyListState,
    pokemonList: List<Pokemon>,
    bufferItemCount: Int = 1
): Boolean {
    val lastVisibleIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
    return lastVisibleIndex >= pokemonList.size - bufferItemCount
}