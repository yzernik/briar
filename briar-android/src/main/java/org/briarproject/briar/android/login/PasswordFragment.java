package org.briarproject.briar.android.login;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.fragment.BaseFragment;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static org.briarproject.briar.android.util.UiUtils.enterPressed;
import static org.briarproject.briar.android.util.UiUtils.hideSoftKeyboard;
import static org.briarproject.briar.android.util.UiUtils.setError;
import static org.briarproject.briar.android.util.UiUtils.showSoftKeyboard;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class PasswordFragment extends BaseFragment implements TextWatcher {

	final static String TAG = PasswordFragment.class.getName();

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private StartupViewModel viewModel;
	private Button signInButton;
	private ProgressBar progress;
	private TextInputLayout input;
	private TextInputEditText password;

	@Override
	public void injectFragment(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_password, container,
						false);

		viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
				.get(StartupViewModel.class);
		viewModel.getPasswordValidated().observeEvent(this, valid -> {
			if (!valid) onPasswordInvalid();
		});

		signInButton = v.findViewById(R.id.btn_sign_in);
		signInButton.setOnClickListener(view -> onSignInButtonClicked());
		progress = v.findViewById(R.id.progress_wheel);
		input = v.findViewById(R.id.password_layout);
		password = v.findViewById(R.id.edit_password);
		password.setOnEditorActionListener((view, actionId, event) -> {
			if (actionId == IME_ACTION_DONE || enterPressed(actionId, event)) {
				onSignInButtonClicked();
				return true;
			}
			return false;
		});
		password.addTextChangedListener(this);
		v.findViewById(R.id.btn_forgotten)
				.setOnClickListener(view -> onForgottenPasswordClick());

		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		showSoftKeyboard(password);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before,
			int count) {
		if (count > 0) setError(input, null, false);
	}

	@Override
	public void afterTextChanged(Editable s) {
	}

	private void onSignInButtonClicked() {
		hideSoftKeyboard(password);
		signInButton.setVisibility(INVISIBLE);
		progress.setVisibility(VISIBLE);
		viewModel.validatePassword(password.getText().toString());
	}

	private void onPasswordInvalid() {
		setError(input, getString(R.string.try_again), true);
		signInButton.setVisibility(VISIBLE);
		progress.setVisibility(INVISIBLE);
		password.setText(null);

		// show the keyboard again
		showSoftKeyboard(password);
	}

	public void onForgottenPasswordClick() {
		// TODO Encapsulate the dialog in a re-usable fragment
		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(),
				R.style.BriarDialogTheme);
		builder.setTitle(R.string.dialog_title_lost_password);
		builder.setMessage(R.string.dialog_message_lost_password);
		builder.setPositiveButton(R.string.cancel, null);
		builder.setNegativeButton(R.string.delete,
				(dialog, which) -> viewModel.deleteAccount());
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

}
