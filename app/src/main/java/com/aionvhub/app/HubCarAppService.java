package com.aionvhub.app;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.model.Action;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.validation.HostValidator;

public class HubCarAppService extends CarAppService {
    @Override
    public void onCreate() {
        super.onCreate();
        HubDiagnostics.event(this, "CarAppService.onCreate");
    }

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        HubDiagnostics.event(this, "createHostValidator → ALLOW_ALL_HOSTS");
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @NonNull
    @Override
    public Session onCreateSession() {
        HubDiagnostics.event(this, "onCreateSession → sessão criada");
        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                HubDiagnostics.event(getCarContext(), "onCreateScreen → tela solicitada pelo host");
                return new HubScreen(getCarContext());
            }
        };
    }

    @Override
    public void onDestroy() {
        HubDiagnostics.event(this, "CarAppService.onDestroy");
        super.onDestroy();
    }

    private static class HubScreen extends Screen {
        HubScreen(@NonNull CarContext carContext) {
            super(carContext);
            HubDiagnostics.event(carContext, "HubScreen construtor concluído");
        }

        @NonNull
        @Override
        public Template onGetTemplate() {
            HubDiagnostics.event(getCarContext(), "onGetTemplate → MessageTemplate entregue");
            try {
                MessageTemplate t = new MessageTemplate.Builder("Integração com Android Auto ativa.")
                        .setTitle("AION V Hub")
                        .setHeaderAction(Action.APP_ICON)
                        .build();
                HubDiagnostics.event(getCarContext(), "MessageTemplate construído com sucesso");
                return t;
            } catch (Throwable e) {
                HubDiagnostics.event(getCarContext(), "ERRO template: " + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()));
                throw e;
            }
        }
    }
}
