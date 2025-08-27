/**
 * Copyright 2025 the original author or authors from the Jeddict project (https://jeddict.github.io/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.jeddict.ai.actions;

import io.github.jeddict.ai.hints.AssistantChatManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.editor.indent.api.Reformat;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 * A popup action that provides AI assistance in the editor. This action is
 * available in the editor's context menu and can be invoked with a shortcut.
 * It opens a chat window with the selected text and allows the user to interact
 * with the AI assistant to modify the code.
 */
@ActionID(
    category = "Edit/Chat",
    id = "io.github.jeddict.ai.actions.AIAssistantPopupAction"
)
@ActionRegistration(
    displayName = "#CTL_AIAssistantPopupAction",
    lazy = false,
    iconInMenu = true,
    iconBase = "icons/logo16.png"
)
@ActionReferences({
    @ActionReference(path = "Editors/Popup", position = 101),
    @ActionReference(path = "Shortcuts", name = "C-QUOTE")
})
@NbBundle.Messages("CTL_AIAssistantPopupAction=AI Assistant")
public final class AIAssistantPopupAction extends BaseContextAction implements ActionListener {

    /**
     * Constructs a new AIAssistantPopupAction.
     */
    public AIAssistantPopupAction() {
        super(Bundle.CTL_AIAssistantPopupAction(), true);
    }

    /**
     * Opens the AI assistant chat window with the selected text from the
     * editor.
     *
     * @param e the action event.
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        AssistantChatManager learnFix;
        JTextComponent editor = EditorRegistry.lastFocusedComponent();
        String selectedText = editor.getSelectedText();
        final StyledDocument document = (StyledDocument) editor.getDocument();

        FileObject file = getFileObject(document);
        int selectionStartPosition = editor.getSelectionStart();
        if (selectedText == null || selectedText.isEmpty()) {
            selectionStartPosition = -1;
            selectedText = "";
        }
        learnFix = new AssistantChatManager(io.github.jeddict.ai.completion.Action.QUERY, file);
        final String text = selectedText;
        final int startLocation = selectionStartPosition;
        learnFix.openChat(null, selectedText, file.getName(), "Chat with AI", content -> {
            NbDocument.runAtomic(document, () -> {
                if (!text.isEmpty()) {
                    insertAndReformat(document, content, startLocation, text.length());
                } else {
                    JTextComponent currenteditor = EditorRegistry.lastFocusedComponent();
                    String currentSelectedText = currenteditor.getSelectedText();
                    final StyledDocument currentDocument = (StyledDocument) currenteditor.getDocument();
                    int currentSelectionStartPosition = currenteditor.getSelectionStart();
                    DataObject currentDO = NbEditorUtilities.getDataObject(currentDocument);
                    if (currentDO != null) {
                        FileObject currentfile = currentDO.getPrimaryFile();
                        if (currentfile != null) {
                            if (currentSelectedText == null || currentSelectedText.isEmpty()) {
                                insertAndReformat(currentDocument, content, currentSelectionStartPosition, 0);
                            } else {
                                insertAndReformat(currentDocument, content, currentSelectionStartPosition, currentSelectedText.length());
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Please select text in the original editor before updating.");
                    }
                }
            });
        });
    }

    /**
     * Inserts the given content into the document and reformats the inserted
     * text.
     *
     * @param document the document to modify.
     * @param content the content to insert.
     * @param startPosition the position at which to insert the content.
     * @param lengthToRemove the length of the text to remove.
     */
    private void insertAndReformat(StyledDocument document, String content, int startPosition, int lengthToRemove) {
        try {
            if (lengthToRemove > 0) {
                document.remove(startPosition, lengthToRemove);
            }
            document.insertString(startPosition, content, null);
            Reformat reformat = Reformat.get(document);
            reformat.lock();
            try {
                reformat.reformat(startPosition, startPosition + content.length());
            } finally {
                reformat.unlock();
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Retrieves the FileObject associated with the given JTextComponent.
     *
     * @param editor The JTextComponent from which to derive the FileObject.
     * @return The FileObject associated with the document, or null if not
     * found.
     */
    private FileObject getFileObject(StyledDocument document) {
        try {
            // Retrieve the DataObject directly from the document's stream property
            DataObject dataObject = NbEditorUtilities.getDataObject(document);
            if (dataObject != null) {
                return dataObject.getPrimaryFile();
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

}
