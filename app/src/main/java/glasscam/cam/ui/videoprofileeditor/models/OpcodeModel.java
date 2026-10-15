package glasscam.cam.ui.videoprofileeditor.models;

import java.util.ArrayList;
import java.util.List;

import glasscam.cam.ui.videoprofileeditor.enums.OpCodes;

public class OpcodeModel extends ButtonModel {
    public OpcodeModel(PopupModel popupModel) {
        super(popupModel);
    }

    @Override
    public List<String> getStrings() {
        List<String> strings = new ArrayList<>();
        for (OpCodes codecs : OpCodes.values())
            strings.add(codecs.name());
        return strings;
    }
}
