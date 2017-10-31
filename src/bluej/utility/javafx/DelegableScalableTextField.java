/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.utility.javafx;

import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.DoubleBinding;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import bluej.utility.Debug;
import threadchecker.OnThread;
import threadchecker.Tag;

public class DelegableScalableTextField<DELEGATE_IDENT> extends ScalableHeightTextField
{
    private final SimpleStyleableDoubleProperty bjMinWidthProperty = new SimpleStyleableDoubleProperty(BJ_MIN_WIDTH_META_DATA);
    /**
     * When we let super class handle nextWord, it may call end, but this can produce the wrong result
     * when we've also overridden end.  This flag indicates whether we are currently in the middle of a next-word call:
     */
    private boolean inNextWord = false;

    private final SimpleStyleableDoubleProperty bjMinWidthProperty() { return bjMinWidthProperty; }

    private static final CssMetaData<DelegableScalableTextField, Number> BJ_MIN_WIDTH_META_DATA =
            JavaFXUtil.cssSize("-bj-min-width", DelegableScalableTextField::bjMinWidthProperty);

    private static final List<CssMetaData <? extends Styleable, ? > > cssMetaDataList =
            JavaFXUtil.extendCss(TextField.getClassCssMetaData())
                    .add(BJ_MIN_WIDTH_META_DATA)
                    .build();

    public static List <CssMetaData <? extends Styleable, ? > > getClassCssMetaData() { return cssMetaDataList; }
    @Override public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() { return cssMetaDataList; }

    private final TextFieldDelegate<DELEGATE_IDENT> delegate;
    private final DELEGATE_IDENT delegateId;
    
    @Override
    public void insertText(int index, String text) {
        delegate.insert(delegateId, index, text);
    }
    
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public boolean deletePreviousChar()
    {
        if (delegate.deleteSelection() || delegate.deletePrevious(delegateId, getCaretPosition(), getCaretPosition() == 0))
        {
            return true;
        }
        else
        {
            return super.deletePreviousChar();
        }
    }

    @Override
    public boolean deleteNextChar()
    {
        if (delegate.deleteSelection() || delegate.deleteNext(delegateId, getCaretPosition(), getCaretPosition() == getLength()))
        {
            return true;
        }
        else
        {
            return super.deleteNextChar();
        }
    }

    @Override
    public void previousWord() {
        if (!delegate.previousWord(delegateId, getCaretPosition() == 0))
            super.previousWord();
    }

    @Override
    public void nextWord() {
        if (!delegate.nextWord(delegateId, getCaretPosition() == getLength()))
        {
            inNextWord = true;
            super.nextWord();
            inNextWord = false;
        }
    }
    
    @Override
    public void endOfNextWord() {
        if (!delegate.endOfNextWord(delegateId, getCaretPosition() == getLength()))
            super.endOfNextWord();
    }



    @Override
    public void backward()
    {
        delegate.deselect();
        if (getCaretPosition() == 0)
        {
            delegate.backwardAtStart(delegateId);
        }
        else
        {
            super.backward();
        }
    }

    @Override
    public void cut()
    {
        if (!delegate.cut())
            super.cut();
    }

    @Override
    public void copy()
    {
        if (!delegate.copy())
            super.copy();
    }

    @Override
    public void forward()
    {
        delegate.deselect();
        if (getCaretPosition() == getText().length())
            delegate.forwardAtEnd(delegateId);
        else
            super.forward();
    }

    @Override
    public void appendText(String text)
    {
        insertText(getText().length(), text);
    }

    @Override
    public void replaceText(IndexRange range, String text)
    {
        replaceText(range.getStart(), range.getEnd(), text);
    }

    @Override
    public void replaceText(int start, int end, String text)
    {
        // Do not delete, we'll handle case there is a selection:
        //deleteText(start, end);
        insertText(start, text);
    }

    @Override
    public void deleteText(IndexRange range)
    {
        super.deleteText(range.getStart(), range.getEnd());
    }

    @Override
    public void deleteText(int start, int end)
    {
        delegate.delete(delegateId, start, end);
        positionCaret(start);
    }

    @Override
    public void selectBackward()
    {
        if (!delegate.selectBackward(delegateId, getCaretPosition())) {
            super.selectBackward();
        }
    }
    
    @Override
    public void deselect() {
        delegate.deselect();
        super.deselect();
    }



    @Override
    public void selectForward()
    {
        if (!delegate.selectForward(delegateId, getCaretPosition(), getCaretPosition() == getLength()))
            super.selectForward();
    }



    // package-visible
    /*
    static class CustomContent implements Content
    {
        public String s = "";
        private final List<ChangeListener<? super String>> listeners = new ArrayList<>();
        private final ExpressionSlot expressionSlot;
        private final List<InvalidationListener> invalidatedListeners = new ArrayList<>();
        
        public CustomContent(ExpressionSlot expressionSlot) {
            this.expressionSlot = expressionSlot;
        }

        @Override
        public String get(int start, int end)
        {
            return s.substring(start, end);
        }

        @Override
        public void insert(int index, String text, boolean notifyListeners) {
            System.out.println("Inserting " + text);
            expressionSlot.insert(this, index, text);
            invalidatedListeners.forEach(l -> l.invalidated(this));
        }

        @Override
        public void delete(int start, int end, boolean notifyListeners)
        {
            String prev = s;
            s = s.substring(0, start) + s.substring(end);
            
            if (notifyListeners && !s.equals(prev))
                listeners.forEach(l -> l.changed(this, prev, s));
        }

        @Override
        public int length() {
            return s.length();
        }

        @Override
        public String get() {
            return s;
        }

        @Override
        public void addListener(ChangeListener<? super String> listener) {
            if (!listeners.contains(listener))
                listeners.add(listener);
        }

        @Override
        public void removeListener(ChangeListener<? super String> listener) {
            listeners.removeIf(l -> l == listener);   
        }

        @Override
        public String getValue() {
            return s;
        }

        @Override
        public void addListener(InvalidationListener listener) {
            if (!invalidatedListeners.contains(listener))
                invalidatedListeners.add(listener);
            
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            invalidatedListeners.removeIf(l -> l == listener);
            
        }
        
    }
    public final TempTextField.CustomContent cc;
    */
    public DelegableScalableTextField(TextFieldDelegate<DELEGATE_IDENT> delegate, DELEGATE_IDENT ident, String content)
    {
        super(content);
        this.delegate = delegate;
        this.delegateId = ident;
        JavaFXUtil.addStyleClass(this, "delegable-scalable-text-field");
        setMinWidth(Region.USE_PREF_SIZE);
        prefWidthProperty().bind(new DoubleBinding() {
            { super.bind(textProperty());
              super.bind(promptTextProperty());
              super.bind(fontProperty());
              super.bind(focusedProperty());
              super.bind(paddingProperty());
              super.bind(bjMinWidthProperty()); }

            @Override
            protected double computeValue()
            {
                double minWidth;
                if (getPromptText().isEmpty() && getText().isEmpty())
                {
                    // Totally empty:
                    return bjMinWidthProperty.get();
                }
                else if (getPromptText().isEmpty() || !getText().isEmpty())
                {
                    minWidth = bjMinWidthProperty.get(); // If we are not showing prompt text, width is min
                }
                else
                {
                    // If we are showing prompt text, measure it:
                    minWidth = JavaFXUtil.measureString(DelegableScalableTextField.this, getPromptText());
                }
                // We need to add a couple of pixels when text is showing; if we are too close
                // to the wire on sizing the field, then JavaFX can start showing the field as totally blank,
                // with all the text scrolled off to the left (presumably because it sees the text field as
                // having overflowed):
                return Math.max(minWidth, 2 + JavaFXUtil.measureString(DelegableScalableTextField.this, getText()));
            }
            
        });
        setOnMousePressed(e -> { 
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) // Double and triple clicks will be handled by the field.
            {
                delegate.clicked();
                delegate.moveTo(e.getSceneX(), e.getSceneY(), true);
                e.consume();
            }
        });
        setOnDragDetected(e -> { e.consume(); });
        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY)
            {
                delegate.selectTo(e.getSceneX(), e.getSceneY());
                e.consume();
            }
        });
        setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY)
            {
                delegate.selected();
                e.consume();
            }
        });
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY)
                e.consume();
        });
        
        caretPositionProperty().addListener((a, b, c) -> delegate.caretMoved());
        
        addEventHandler(KeyEvent.KEY_PRESSED, e -> { if (e.getCode() == KeyCode.ESCAPE) delegate.escape(); });
        
    }
    
    @Override
    public void selectNextWord()
    {
        if (!delegate.selectNextWord(delegateId))
            super.selectNextWord();
    }
    
    @Override
    public void selectEndOfNextWord()
    {
        if (!delegate.selectNextWord(delegateId))
            super.selectEndOfNextWord();
    }

    @Override
    public void selectPreviousWord()
    {
        if (!delegate.selectPreviousWord(delegateId))
            super.selectPreviousWord();
    }

    @Override
    public void selectAll() {
        if (!delegate.selectAll(delegateId))
            super.selectAll();
    }
    @Override
    public void home() {
        delegate.deselect();
        if (!delegate.home(delegateId))
            super.home();
    }

    @Override
    public void end() {
        delegate.deselect();
        if (!delegate.end(delegateId, inNextWord))
            super.end();
    }



    @Override
    public void selectHome() {
        if (!delegate.selectHome(delegateId, getCaretPosition()))
            super.selectHome();
    }



    @Override
    public void selectEnd() {
        if (!delegate.selectEnd(delegateId, getCaretPosition()))
            super.selectEnd();
    }



    @Override
    public void paste() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString())
        {
            delegate.deleteSelection();
            insertText(getCaretPosition(), clipboard.getString());
        }
    }


    public double calculateSceneX(int caretPos)
    {
        double borderLeft = 0;
        if (getBorder() != null && getBorder().getInsets() != null)
            borderLeft = getBorder().getInsets().getLeft();
        double localX = getPadding().getLeft() + borderLeft +
                JavaFXUtil.measureString(this, getText().substring(0, Math.min(caretPos, getText().length())), true, false);
        // Alternatively above, rather than true, false, we could pass:
        // caretPos != 0, caretPos == getLength()
        // to make the positions extend to the edge of the text field when looking up the edge.
        return localToScene(localX, 0).getX();
    }
}