package tools.dynamia.chronos.ui;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
import org.zkoss.zul.Div;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listfoot;
import org.zkoss.zul.Listfooter;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;
import tools.dynamia.zk.BindingComponentIndex;
import tools.dynamia.zk.ComponentAliasIndex;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inline editable key/value grid bound directly to a {@code Map<String, String>} bean property.
 * Register as a form field with {@code component: headerseditor} to edit HTTP headers (or any
 * other string map) declaratively, without a dedicated ZUL page or view model.
 */
public class HeadersEditor extends Div {

    static {
        BindingComponentIndex.getInstance().put("value", HeadersEditor.class);
        ComponentAliasIndex.getInstance().add("headerseditor", HeadersEditor.class);
    }

    private final Listbox listbox = new Listbox();
    private final ListModelList<Entry> model = new ListModelList<>();

    public HeadersEditor() {
        initUI();
    }

    private void initUI() {
        setHflex("1");

        listbox.setModel(model);
        listbox.setHflex("1");

        Listhead listhead = new Listhead();
        listhead.setSizable(true);
        new Listheader("Header").setParent(listhead);
        new Listheader("Value").setParent(listhead);
        Listheader actionsHeader = new Listheader();
        actionsHeader.setWidth("40px");
        actionsHeader.setParent(listhead);
        listhead.setParent(listbox);

        listbox.setItemRenderer((Listitem item, Entry entry, int index) -> renderRow(item, entry));

        Listfoot listfoot = new Listfoot();
        Listfooter listfooter = new Listfooter();
        listfooter.setSpan(3);
        A addLink = new A("Add header");
        addLink.addEventListener(Events.ON_CLICK, e -> {
            // Don't post a change event here: the new row has a blank key, and getValue()
            // filters blank keys out, so an immediate save+reload round trip would wipe the
            // row before the user gets a chance to type anything into it. The row still
            // shows up live because the Listbox is bound to this same ListModelList instance.
            model.add(new Entry("", ""));
        });
        addLink.setParent(listfooter);
        listfooter.setParent(listfoot);
        listfoot.setParent(listbox);

        appendChild(listbox);
    }

    private void renderRow(Listitem item, Entry entry) {
        item.setValue(entry);

        Listcell keyCell = new Listcell();
        Textbox keyBox = new Textbox(entry.key);
        keyBox.setPlaceholder("key");
        keyBox.setHflex("1");
        keyBox.addEventListener(Events.ON_CHANGE, e -> {
            entry.key = keyBox.getValue();
            notifyValueChanged();
        });
        keyBox.setParent(keyCell);
        keyCell.setParent(item);

        Listcell valueCell = new Listcell();
        Textbox valueBox = new Textbox(entry.value);
        valueBox.setPlaceholder("value");
        valueBox.setHflex("1");
        valueBox.addEventListener(Events.ON_CHANGE, e -> {
            entry.value = valueBox.getValue();
            notifyValueChanged();
        });
        valueBox.setParent(valueCell);
        valueCell.setParent(item);

        Listcell actionsCell = new Listcell();
        A removeLink = new A();
        removeLink.setIconSclass("z-icon-remove");
        removeLink.setSclass("text-danger");
        removeLink.addEventListener(Events.ON_CLICK, e -> {
            model.remove(entry);
            notifyValueChanged();
        });
        removeLink.setParent(actionsCell);
        actionsCell.setParent(item);
    }

    private void notifyValueChanged() {
        Events.postEvent(new Event(Events.ON_CHANGE, this, getValue()));
    }

    public void setValue(Map<String, String> value) {
        model.clear();
        if (value != null) {
            value.forEach((key, val) -> model.add(new Entry(key, val)));
        }
    }

    public Map<String, String> getValue() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Entry entry : model) {
            if (entry.key != null && !entry.key.isBlank()) {
                result.put(entry.key, entry.value != null ? entry.value : "");
            }
        }
        return result;
    }

    public static class Entry {
        private String key;
        private String value;

        public Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
