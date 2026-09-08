package com.stremio.mobile.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalAutofillManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.core.theme.StremioBackground
import com.stremio.mobile.auth.FacebookLoginBridge
import com.stremio.mobile.presentation.components.StremioMark
import com.stremio.mobile.presentation.components.ThemedButton
import com.stremio.mobile.presentation.components.ThemedTextButton
import com.stremio.mobile.presentation.components.LocalIsTv
import com.stremio.mobile.presentation.components.tvClickable
import com.stremio.mobile.presentation.components.tvFocusIndicator
import com.stremio.mobile.presentation.components.tvNotFocusable

enum class AuthScreen {
    Intro,
    Login,
    Signup,
}

@Composable
fun AuthFlow(
    isLoading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onFacebookLogin: (String) -> Unit,
    onFacebookLoginError: (String) -> Unit,
    onSignup: (String, String, Boolean) -> Unit,
    onClearError: () -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(AuthScreen.Intro) }

    when (screen) {
        AuthScreen.Intro -> IntroScreen(
            isLoading = isLoading,
            error = error,
            onLoginClicked = {
                onClearError()
                screen = AuthScreen.Login
            },
            onFacebookLogin = onFacebookLogin,
            onFacebookLoginError = onFacebookLoginError,
            onClearError = onClearError,
            onSignupClicked = {
                onClearError()
                screen = AuthScreen.Signup
            },
        )

        AuthScreen.Login -> LoginScreen(
            isLoading = isLoading,
            error = error,
            onBack = {
                onClearError()
                screen = AuthScreen.Intro
            },
            onSubmit = onLogin,
            onSignup = {
                onClearError()
                screen = AuthScreen.Signup
            },
        )

        AuthScreen.Signup -> SignupScreen(
            isLoading = isLoading,
            error = error,
            onBack = {
                onClearError()
                screen = AuthScreen.Intro
            },
            onSubmit = onSignup,
            onLogin = {
                onClearError()
                screen = AuthScreen.Login
            },
        )
    }
}

@Composable
private fun IntroScreen(
    isLoading: Boolean,
    error: String?,
    onLoginClicked: () -> Unit,
    onFacebookLogin: (String) -> Unit,
    onFacebookLoginError: (String) -> Unit,
    onClearError: () -> Unit,
    onSignupClicked: () -> Unit,
) {
    val context = LocalContext.current
    val isTv = LocalIsTv.current
    val signUpFocusRequester = FocusRequester()
    val facebookFocusRequester = FocusRequester()
    val loginFocusRequester = FocusRequester()

    if (isTv) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            signUpFocusRequester.requestFocus()
        }
    }

    DisposableEffect(onFacebookLogin, onFacebookLoginError) {
        val callback = object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                val token = result.accessToken.token
                if (token.isBlank()) {
                    onFacebookLoginError("Facebook did not return an access token.")
                } else {
                    onFacebookLogin(token)
                }
            }

            override fun onCancel() {
                onFacebookLoginError("Facebook login was cancelled.")
            }

            override fun onError(error: FacebookException) {
                onFacebookLoginError(error.localizedMessage ?: "Facebook login failed")
            }
        }
        LoginManager.getInstance().registerCallback(FacebookLoginBridge.callbackManager, callback)
        onDispose {
            LoginManager.getInstance().unregisterCallback(FacebookLoginBridge.callbackManager)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF7D58F4),
                        0.18f to Color(0xFF403075),
                        0.42f to StremioBackground,
                        1.0f to StremioBackground,
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 31.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(if (isTv) 12.dp else 18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                StremioMark(modifier = Modifier.size(58.dp))
                Text(
                    text = "stremio",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
            Spacer(modifier = Modifier.height(if (isTv) 36.dp else 68.dp))
            Text(
                text = "Freedom to Stream",
                color = Color.White,
                fontSize = 35.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "All the video content you enjoy in one place",
                color = Color(0xFFD7D0F0),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(if (isTv) 24.dp else 34.dp))
            AuthButton(
                text = "Sign up",
                enabled = !isLoading,
                onClick = onSignupClicked,
                modifier = Modifier
                    .focusRequester(signUpFocusRequester)
                    .focusProperties {
                        down = facebookFocusRequester
                    },
            )
            AuthButton(
                text = if (isLoading) "Connecting..." else "Continue with Facebook",
                containerColor = Color(0xFF126FFF),
                enabled = !isLoading,
                modifier = Modifier
                    .focusRequester(facebookFocusRequester)
                    .focusProperties {
                        up = signUpFocusRequester
                        down = loginFocusRequester
                    },
                onClick = {
                    onClearError()
                    val activity = context.findActivity()
                    if (activity == null) {
                        onFacebookLoginError("Facebook login requires an active app screen.")
                    } else {
                        LoginManager.getInstance().logInWithReadPermissions(activity, listOf("email"))
                    }
                },
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(28.dp),
                    color = Color.White,
                )
            }
            if (error != null) {
                Text(
                    text = error,
                    color = Color(0xFFFFC66D),
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                )
            }
            Spacer(modifier = Modifier.height(if (isTv) 12.dp else 24.dp))
            Text(
                text = "Already have an account?",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = Color(0xFF8E869F),
                fontSize = 18.sp,
            )
            ThemedTextButton(
                text = "Log in",
                onClick = onLoginClicked,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .focusRequester(loginFocusRequester)
                    .focusProperties {
                        up = facebookFocusRequester
                    },
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun LoginScreen(
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onSignup: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val autofillManager = LocalAutofillManager.current
    val emailFocus = remember { FocusRequester() }
    com.stremio.mobile.presentation.components.TvRequestFocus(emailFocus)

    AuthFormScaffold(
        title = "Log in",
        subtitle = "Use your Stremio account to sync your library, addons, and continue watching.",
        isLoading = isLoading,
        error = error,
        onBack = onBack,
    ) {
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            modifier = Modifier
                .focusRequester(emailFocus)
                .semantics { contentType = ContentType.EmailAddress }
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true,
            modifier = Modifier.semantics { contentType = ContentType.Password }
        )
        AuthButton(
            text = if (isLoading) "Logging in..." else "Log in",
            enabled = !isLoading,
            onClick = {
                autofillManager?.commit()
                onSubmit(email, password)
            },
        )
        TextLink(
            text = "Don't have an account? Sign up",
            onClick = onSignup,
        )
    }
}

@Composable
private fun SignupScreen(
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSubmit: (String, String, Boolean) -> Unit,
    onLogin: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordConfirm by rememberSaveable { mutableStateOf("") }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    var acceptedPrivacy by rememberSaveable { mutableStateOf(false) }
    var marketing by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    val autofillManager = LocalAutofillManager.current
    val emailFocus = remember { FocusRequester() }
    com.stremio.mobile.presentation.components.TvRequestFocus(emailFocus)

    AuthFormScaffold(
        title = "Sign up",
        subtitle = "Create a Stremio account to keep your library and addon setup available on every device.",
        isLoading = isLoading,
        error = localError ?: error,
        onBack = onBack,
    ) {
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            modifier = Modifier
                .focusRequester(emailFocus)
                .semantics { contentType = ContentType.EmailAddress }
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true,
            modifier = Modifier.semantics { contentType = ContentType.NewPassword }
        )
        AuthTextField(
            value = passwordConfirm,
            onValueChange = { passwordConfirm = it },
            label = "Confirm password",
            isPassword = true,
            modifier = Modifier.semantics { contentType = ContentType.NewPassword }
        )
        AuthCheckRow(
            checked = acceptedTerms,
            text = "I accept the Terms of Service",
            onCheckedChange = { acceptedTerms = it },
        )
        AuthCheckRow(
            checked = acceptedPrivacy,
            text = "I accept the Privacy Policy",
            onCheckedChange = { acceptedPrivacy = it },
        )
        AuthCheckRow(
            checked = marketing,
            text = "Receive product updates by email",
            onCheckedChange = { marketing = it },
        )
        AuthButton(
            text = if (isLoading) "Creating account..." else "Sign up",
            enabled = !isLoading,
            onClick = {
                localError = when {
                    password != passwordConfirm -> "Passwords do not match"
                    !acceptedTerms -> "You must accept the Terms of Service"
                    !acceptedPrivacy -> "You must accept the Privacy Policy"
                    else -> null
                }
                if (localError == null) {
                    autofillManager?.commit()
                    onSubmit(email, password, marketing)
                }
            },
        )
        TextLink(
            text = "Already have an account? Log in",
            onClick = onLogin,
        )
    }
}

@Composable
private fun AuthFormScaffold(
    title: String,
    subtitle: String,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val backFocusRequester = FocusRequester()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 26.dp),
        contentPadding = PaddingValues(top = 30.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextLink(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.focusRequester(backFocusRequester),
            )
            Spacer(modifier = Modifier.height(42.dp))
            StremioMark(modifier = Modifier.size(46.dp))
            Spacer(modifier = Modifier.height(34.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = subtitle,
                color = MutedText,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                content()
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(28.dp),
                        color = AccentPurple,
                    )
                }
                if (error != null) {
                    Text(
                        text = error,
                        color = Color(0xFFFFC66D),
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .tvFocusIndicator(RoundedCornerShape(12.dp)),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = AccentPurple,
            unfocusedBorderColor = Color(0xFF555160),
            focusedLabelColor = MutedText,
            unfocusedLabelColor = MutedText,
            cursorColor = AccentPurple,
        ),
    )
}

@Composable
private fun AuthCheckRow(
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(12.dp)) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.tvNotFocusable(),
            colors = CheckboxDefaults.colors(
                checkedColor = AccentPurple,
                uncheckedColor = MutedText,
                checkmarkColor = Color.White,
            ),
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun AuthButton(
    text: String,
    containerColor: Color = AccentPurple,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ThemedButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        containerColor = containerColor,
    )
}

@Composable
private fun TextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .tvClickable(shape = RoundedCornerShape(8.dp), onClick = onClick),
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )
}
