/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.hansolo.fx.tgauge;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;


/**
 *
 * @author Gerrit Grunwald <han.solo.gg at gmail.com>
 */
public class Section {
    private DoubleProperty        start;
    private DoubleProperty        stop;
    private ObjectProperty<Color> color;
    private ObjectProperty<Arc>   area;

    public Section(final double START, final double STOP, final Color COLOR) {
        start = new SimpleDoubleProperty(START);
        stop  = new SimpleDoubleProperty(STOP);
        color  = new SimpleObjectProperty<>(COLOR);
        area  = new SimpleObjectProperty<>(new Arc());
        validate();
    }

    public double getStart() {
        return start.get();
    }
    public void setStart(final double START) {
        start.set(START);
        validate();
    }
    public DoubleProperty startProperty() {
        return start;
    }

    public double getStop() {
        return stop.get();
    }
    public void setStop(final double STOP) {
        stop.set(STOP);
        validate();
    }
    public DoubleProperty stopProperty() {
        return stop;
    }

    public Paint getColor() {
        return color.get();
    }
    public void setFill(final Color COLOR) {
        color.set(COLOR);
    }
    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    public Arc getArea() {
        return area.get();
    }
    public void setArea(final Arc AREA) {
        area.set(AREA);
    }
    public ObjectProperty<Arc> areaProperty() {
        return area;
    }

    public boolean contains(final double VALUE) {
        return ((Double.compare(VALUE, start.get()) >= 0 && Double.compare(VALUE, stop.get()) <= 0));
    }

    private void validate() {
        if (getStart() > getStop()) setStart(getStop() - 1);
        if (getStop() < getStart()) setStop(getStart() + 1);
    }
}
