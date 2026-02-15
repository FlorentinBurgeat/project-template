import { useState } from "react";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import type { UserProfileFormFieldsProps } from "keycloakify/login/UserProfileFormFieldsProps";

type RegisterProps = PageProps<Extract<KcContext, { pageId: "register.ftl" }>, I18n> & {
  UserProfileFormFields: (props: UserProfileFormFieldsProps) => JSX.Element;
  doMakeUserConfirmPassword: boolean;
};

export default function Register(props: RegisterProps) {
  const {
    kcContext,
    i18n,
    doUseDefaultCss,
    classes,
    Template,
    UserProfileFormFields,
    doMakeUserConfirmPassword,
  } = props;

  const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });
  const { msg, msgStr } = i18n;
  const { url, messagesPerField, recaptchaRequired, recaptchaSiteKey } = kcContext;

  const [isFormSubmittable, setIsFormSubmittable] = useState(false);

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      headerNode={msg("registerTitle")}
      displayMessage={messagesPerField.exists("global")}
      displayRequiredFields
    >
      <form
        id="kc-register-form"
        className={kcClsx("kcFormClass")}
        action={url.registrationAction}
        method="post"
      >
        <UserProfileFormFields
          kcContext={kcContext}
          i18n={i18n}
          kcClsx={kcClsx}
          onIsFormSubmittableValueChange={setIsFormSubmittable}
          doMakeUserConfirmPassword={doMakeUserConfirmPassword}
        />

        {recaptchaRequired && recaptchaSiteKey && (
          <div className="form-group">
            <div className={kcClsx("kcInputWrapperClass")}>
              <div className="g-recaptcha" data-size="compact" data-sitekey={recaptchaSiteKey} />
            </div>
          </div>
        )}

        <div className={kcClsx("kcFormGroupClass")}>
          <div id="kc-form-options" className={kcClsx("kcFormOptionsClass")}>
            <div className={kcClsx("kcFormOptionsWrapperClass")}>
              <span>
                <a href={url.loginUrl}>{msg("backToLogin")}</a>
              </span>
            </div>
          </div>

          <div id="kc-form-buttons" className={kcClsx("kcFormButtonsClass")}>
            <input
              className={kcClsx(
                "kcButtonClass",
                "kcButtonPrimaryClass",
                "kcButtonBlockClass",
                "kcButtonLargeClass"
              )}
              type="submit"
              value={msgStr("doRegister")}
              disabled={!isFormSubmittable}
            />
          </div>
        </div>
      </form>
    </Template>
  );
}
