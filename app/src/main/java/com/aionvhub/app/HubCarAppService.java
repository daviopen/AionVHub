package com.aionvhub.app;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.model.Action;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.validation.HostValidator;

public class HubCarAppService extends CarAppService {
    @NonNull
    @Override
    public HostValidator createHostValidator() {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @NonNull
    @Override
    public Session onCreateSession() {
        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                return new HubScreen(getCarContext());
            }
        };
    }

    private static class HubScreen extends Screen {
        HubScreen(@NonNull CarContext carContext) {
            super(carContext);
        }

        @NonNull
        @Override
        public Template onGetTemplate() {
            Row statusRow = new Row.Builder()
                    .setTitle("AION V Hub conectado")
                    .addText("Sessão Android Auto ativa")
                    .build();

            Row infoRow = new Row.Builder()
                    .setTitle("Versão 0.3.0")
                    .addText("Teste de integração com Android Auto")
                    .build();

            Pane pane = new Pane.Builder()
                    .addRow(statusRow)
                    .addRow(infoRow)
                    .build();

            return new PaneTemplate.Builder(pane)
                    .setTitle("AION V Hub")
                    .setHeaderAction(Action.APP_ICON)
                    .build();
        }
    }
}
