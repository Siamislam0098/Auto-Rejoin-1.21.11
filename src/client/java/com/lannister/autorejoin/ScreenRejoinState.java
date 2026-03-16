package com.lannister.autorejoin;

import net.minecraft.client.gui.widget.ButtonWidget;

public interface ScreenRejoinState {
    ButtonWidget autoRejoin$getCancelBtn();
    void autoRejoin$setCancelBtn(ButtonWidget btn);
}