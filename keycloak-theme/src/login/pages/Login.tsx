import { useState } from "react";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export default function Login(
  props: PageProps<Extract<KcContext, { pageId: "login.ftl" }>, I18n>
) {
  const { kcContext, i18n, doUseDefaultCss, classes, Template } = props;
  const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });

  const { msg, msgStr } = i18n;
  const {
    social,
    realm,
    url,
    usernameHidden,
    login,
    registrationDisabled,
    messagesPerField,
  } = kcContext;

  const [isLoginButtonDisabled, setIsLoginButtonDisabled] = useState(false);

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      displayMessage={!messagesPerField.existsError("username", "password")}
      headerNode={msg("loginAccountTitle")}
      displayInfo={
        realm.password && realm.registrationAllowed && !registrationDisabled
      }
      infoNode={
        <div id="kc-registration-container">
          <div id="kc-registration">
            <span>
              {msg("noAccount")}{" "}
              <a tabIndex={8} href={url.registrationUrl} className="theme-link">
                {msg("doRegister")}
              </a>
            </span>
          </div>
        </div>
      }
      socialProvidersNode={
        social?.providers !== undefined &&
        social.providers.length > 0 && (
          <div id="kc-social-providers" className={kcClsx("kcFormSocialAccountSectionClass")}>
            <hr />
            <h2>{msg("identity-provider-login-label")}</h2>
            <ul className={kcClsx("kcFormSocialAccountListClass", social.providers.length > 3 ? "kcFormSocialAccountListGridClass" : undefined)}>
              {social.providers.map((p) => (
                <li key={p.alias}>
                  <a
                    id={`social-${p.alias}`}
                    className={kcClsx("kcFormSocialAccountListButtonClass", social.providers!.length > 3 ? "kcFormSocialAccountGridItem" : undefined)}
                    type="button"
                    href={p.loginUrl}
                  >
                    {p.iconClasses && (
                      <i className={kcClsx("kcCommonLogoIdP") + " " + p.iconClasses} aria-hidden="true"></i>
                    )}
                    <span className={kcClsx("kcFormSocialAccountNameClass", p.iconClasses ? undefined : "kcFormSocialAccountNameRemClass")}>
                      {p.displayName}
                    </span>
                  </a>
                </li>
              ))}
            </ul>
          </div>
        )
      }
    >
      <div id="kc-form">
        <div id="kc-form-wrapper">
          {realm.password && (
            <form
              id="kc-form-login"
              onSubmit={() => {
                setIsLoginButtonDisabled(true);
                return true;
              }}
              action={url.loginAction}
              method="post"
            >
              {!usernameHidden && (
                <div className={kcClsx("kcFormGroupClass")}>
                  <label htmlFor="username" className={kcClsx("kcLabelClass")}>
                    {!realm.loginWithEmailAllowed
                      ? msg("username")
                      : !realm.registrationEmailAsUsername
                        ? msg("usernameOrEmail")
                        : msg("email")}
                  </label>
                  <input
                    tabIndex={2}
                    id="username"
                    className={kcClsx("kcInputClass")}
                    name="username"
                    defaultValue={login.username ?? ""}
                    type="text"
                    autoFocus
                    autoComplete="username"
                    aria-invalid={messagesPerField.existsError("username")}
                  />
                  {messagesPerField.existsError("username") && (
                    <span
                      id="input-error-username"
                      className={kcClsx("kcInputErrorMessageClass")}
                      aria-live="polite"
                      dangerouslySetInnerHTML={{
                        __html: messagesPerField.getFirstError("username"),
                      }}
                    />
                  )}
                </div>
              )}

              <div className={kcClsx("kcFormGroupClass")}>
                <label htmlFor="password" className={kcClsx("kcLabelClass")}>
                  {msg("password")}
                </label>
                <input
                  tabIndex={3}
                  id="password"
                  className={kcClsx("kcInputClass")}
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  aria-invalid={messagesPerField.existsError("username", "password")}
                />
                {messagesPerField.existsError("username", "password") && (
                  <span
                    id="input-error-password"
                    className={kcClsx("kcInputErrorMessageClass")}
                    aria-live="polite"
                    dangerouslySetInnerHTML={{
                      __html: messagesPerField.getFirstError("username", "password"),
                    }}
                  />
                )}
              </div>

              <div className={kcClsx("kcFormGroupClass", "kcFormSettingClass")}>
                <div id="kc-form-options">
                  {realm.rememberMe && !usernameHidden && (
                    <div className="checkbox">
                      <label>
                        <input
                          tabIndex={5}
                          id="rememberMe"
                          name="rememberMe"
                          type="checkbox"
                          defaultChecked={!!login.rememberMe}
                        />
                        {msg("rememberMe")}
                      </label>
                    </div>
                  )}
                </div>
                <div className={kcClsx("kcFormOptionsWrapperClass")}>
                  {realm.resetPasswordAllowed && (
                    <span>
                      <a tabIndex={6} href={url.loginResetCredentialsUrl}>
                        {msg("doForgotPassword")}
                      </a>
                    </span>
                  )}
                </div>
              </div>

              <div id="kc-form-buttons" className={kcClsx("kcFormGroupClass")}>
                <input type="hidden" id="id-hidden-input" name="credentialId" value={kcContext.auth.selectedCredential} />
                <input
                  tabIndex={7}
                  disabled={isLoginButtonDisabled}
                  className={kcClsx("kcButtonClass", "kcButtonPrimaryClass", "kcButtonBlockClass", "kcButtonLargeClass")}
                  name="login"
                  id="kc-login"
                  type="submit"
                  value={msgStr("doLogIn")}
                />
              </div>
            </form>
          )}
        </div>
      </div>
    </Template>
  );
}
