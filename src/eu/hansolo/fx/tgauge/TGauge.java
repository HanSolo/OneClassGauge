/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.hansolo.fx.tgauge;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.GroupBuilder;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.DropShadowBuilder;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.InnerShadowBuilder;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBuilder;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;


/**
 *
 * @author Gerrit Grunwald <han.solo.gg at gmail.com>
 */
public class TGauge extends Region {
    private static final double     DEFAULT_WIDTH   = 400;
    private static final double     DEFAULT_HEIGHT  = 400;
    private static final double     MINIMUM_WIDTH   = 5;
    private static final double     MINIMUM_HEIGHT  = 5;
    private static final double     MAXIMUM_WIDTH   = 1024;
    private static final double     MAXIMUM_HEIGHT  = 1024;
    private static final double     ANGLE_RANGE     = 300;
    private static final double     ROTATION_OFFSET = 150;
    private static final double     TIME_TO_VALUE   = 2500;
    private static final double     FRACTION        = 2;
    private static double           aspectRatio;
    private double                  size;
    private double                  width;
    private double                  height;
    private DoubleProperty          value;
    private DoubleProperty          minValue;
    private DoubleProperty          maxValue;
    private DoubleProperty          minMeasuredValue;
    private DoubleProperty          maxMeasuredValue;
    private DoubleProperty          threshold;
    private BooleanProperty         thresholdExceeded;
    private BooleanProperty         thresholdBehaviorInverted;
    private BooleanProperty         animated;
    private BooleanProperty         backgroundVisible;
    private Timeline                timeline;
    private double                  angleStep;
    private Pane                    pane;
    private Region                  frame;
    private Region                  background;
    private Region                  needle;
    private Group                   pointerGroup;
    private Rotate                  needleRotate;
    private Region                  knob;
    private Text                    unitText;
    private StringProperty          unitString;
    private Font                    unitFont;
    private Text                    valueText;
    private Font                    valueFont;
    private List<Text>              tickLabels;
    private Font                    tickLabelFont;
    private Group                   tickLabelGroup;
    private Canvas                  alertIndicator;
    private InnerShadow             gaugeKnobInnerShadow0;
    private InnerShadow             gaugeKnobInnerShadow1;
    private DropShadow              gaugeKnobDropShadow;
    private DropShadow              pointerShadow;
    private DropShadow              textDropShadow;
    private ObservableList<Section> sections;
    private Group                   sectionGroup;
    private ChangeListener<Number>  sizeListener;


    // ******************** Constructors **************************************
    public TGauge() {
        getStylesheets().add(getClass().getResource("tgauge.css").toExternalForm());
        getStyleClass().add("tgauge");
        aspectRatio               = DEFAULT_HEIGHT / DEFAULT_WIDTH;
        pane                      = new Pane();
        unitString                = new SimpleStringProperty("°C");
        sections                  = FXCollections.observableArrayList();
        value                     = new SimpleDoubleProperty(0);
        minValue                  = new SimpleDoubleProperty(0);
        maxValue                  = new SimpleDoubleProperty(100);
        minMeasuredValue          = new SimpleDoubleProperty(100);
        maxMeasuredValue          = new SimpleDoubleProperty(0);
        threshold                 = new SimpleDoubleProperty(50);
        thresholdExceeded         = new SimpleBooleanProperty(false);
        thresholdBehaviorInverted = new SimpleBooleanProperty(false);
        angleStep                 = ANGLE_RANGE / (getMaxValue() - getMinValue());
        animated                  = new SimpleBooleanProperty(true);
        backgroundVisible         = new SimpleBooleanProperty(true);
        timeline                  = new Timeline();
        needleRotate              = new Rotate(-ROTATION_OFFSET);
        tickLabels                = new ArrayList<>();
        init();
        initGraphics();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void init() {
        if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 ||
            Double.compare(getWidth(), 0.0) <= 0 || Double.compare(getHeight(), 0.0) <= 0) {
            if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                setPrefSize(getPrefWidth(), getPrefHeight());
            } else {
                setPrefSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            }
        }

        if (Double.compare(getMinWidth(), 0.0) <= 0 || Double.compare(getMinHeight(), 0.0) <= 0) {
            setMinSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        }

        if (Double.compare(getMaxWidth(), 0.0) <= 0 || Double.compare(getMaxHeight(), 0.0) <= 0) {
            setMaxSize(MAXIMUM_WIDTH, MAXIMUM_HEIGHT);
        }

        if (getPrefWidth() != DEFAULT_WIDTH || getPrefHeight() != DEFAULT_HEIGHT) {
            aspectRatio = getPrefHeight() / getPrefWidth();
        }

        sizeListener = new ChangeListener<Number> () {
            @Override public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                resize();
                aspectRatio = getPrefHeight() / getPrefWidth();
            }
        };
    }

    private void initGraphics() {
        frame = new Region();
        frame.getStyleClass().setAll("frame");

        background = new Region();
        background.getStyleClass().setAll("background");

        sectionGroup = new Group();
        for (final Section section : getSections()) {
            sectionGroup.getChildren().add(section.getArea());
        }

        textDropShadow = DropShadowBuilder.create()
                                          .radius(DEFAULT_WIDTH * 0.005)
                                          .offsetY(DEFAULT_WIDTH * 0.0025)
                                          .blurType(BlurType.GAUSSIAN)
                                          .color(Color.rgb(0, 0, 0, 0.65))
                                          .build();

        tickLabelFont = Font.font("Verdana", FontWeight.BOLD, DEFAULT_WIDTH * 0.04);
        for (double i = getMinValue() ; Double.compare(i, getMaxValue()) <= 0 ; i += FRACTION) {
            Text tickLabel = TextBuilder.create()
                                        .font(tickLabelFont)
                                        .text(String.format("%.0f", i))
                                        .effect(textDropShadow)
                                        .styleClass("tick-label")
                                        .build();
            tickLabels.add(tickLabel);
        }
        tickLabelGroup = GroupBuilder.create()
                                     .children(tickLabels)
                                     .build();

        needle = new Region();
        needle.setEffect(new InnerShadow());
        needle.getStyleClass().setAll("needle");
        needle.getTransforms().setAll(needleRotate);

        pointerGroup = new Group(needle);
        pointerGroup.setEffect(pointerShadow);
        pointerShadow = DropShadowBuilder.create()
                                         .radius(0.05 * DEFAULT_WIDTH)
                                         .offsetY(0.02 * DEFAULT_WIDTH)
                                         .blurType(BlurType.GAUSSIAN)
                                         .color(Color.rgb(0, 0, 0, 0.65))
                                         .build();

        pointerGroup = new Group(needle);
        pointerGroup.setEffect(pointerShadow);

        knob = new Region();
        knob.getStyleClass().setAll("knob");

        gaugeKnobInnerShadow0 = InnerShadowBuilder.create()
                                                  .offsetY(2.0)
                                                  .radius(0.005 * DEFAULT_WIDTH)
                                                  .color(Color.rgb(255, 255, 255, 0.45))
                                                  .blurType(BlurType.GAUSSIAN)
                                                  .build();
        gaugeKnobInnerShadow1 = InnerShadowBuilder.create()
                                                  .offsetY(-2.0)
                                                  .radius(0.005 * DEFAULT_WIDTH)
                                                  .color(Color.rgb(0, 0, 0, 0.65))
                                                  .blurType(BlurType.GAUSSIAN)
                                                  .input(gaugeKnobInnerShadow0)
                                                  .build();
        gaugeKnobDropShadow = DropShadowBuilder.create()
                                               .offsetY(0.02 * DEFAULT_WIDTH)
                                               .radius(0.05 * DEFAULT_WIDTH)
                                               .color(Color.rgb(0, 0, 0, 0.65))
                                               .blurType(BlurType.GAUSSIAN)
                                               .input(gaugeKnobInnerShadow1)
                                               .build();
        knob.setEffect(gaugeKnobDropShadow);

        unitFont = Font.font("Verdana", FontWeight.NORMAL, DEFAULT_WIDTH * 0.16);
        unitText = TextBuilder.create()
                              .textOrigin(VPos.CENTER)
                              .textAlignment(TextAlignment.CENTER)
                              .font(unitFont)
                              .effect(textDropShadow)
                              .styleClass("unit")
                              .build();
        unitText.textProperty().bind(unitString);

        valueFont = Font.font("Verdana", FontWeight.NORMAL, DEFAULT_WIDTH * 0.07);
        valueText = TextBuilder.create()
                               .text("0.00")
                               .textOrigin(VPos.CENTER)
                               .textAlignment(TextAlignment.CENTER)
                               .effect(textDropShadow)
                               .font(valueFont)
                               .styleClass("value")
                               .build();

        alertIndicator = new Canvas();
        alertIndicator.visibleProperty().bind(thresholdExceeded);
        drawAlertIndicator(0.124 * DEFAULT_WIDTH, 0.108 * DEFAULT_HEIGHT, Color.RED);

        pane.getChildren().setAll(frame,
                                  background,
                                  sectionGroup,
                                  tickLabelGroup,
                                  valueText,
                                  alertIndicator,
                                  pointerGroup,
                                  knob,
                                  unitText);

        getChildren().setAll(pane);
        resize();
    }

    private void registerListeners() {
        widthProperty().addListener(sizeListener);
        heightProperty().addListener(sizeListener);
        prefWidthProperty().addListener(sizeListener);
        prefHeightProperty().addListener(sizeListener);
        backgroundVisible.addListener(new InvalidationListener() {
            @Override public void invalidated(Observable o) {
                frame.setVisible(isBackgroundVisible());
                background.setVisible(isBackgroundVisible());
            }
        });
        valueProperty().addListener(new InvalidationListener(){
            @Override public void invalidated(Observable o) {
                valueText.setText(String.format("%.2f", getValue()));
                rotateNeedle();
                if (getValue() < getMinMeasuredValue()) {
                    setMinMeasuredValue(getValue());
                } else if (getValue() > getMaxMeasuredValue()) {
                    setMaxMeasuredValue(getValue());
                }
                if (isThresholdBehaviorInverted() && getValue() < getThreshold()) {
                    setThresholdExceeded(true);
                } else if (!isThresholdBehaviorInverted() && getValue() > getThreshold()) {
                    setThresholdExceeded(true);
                } else {
                    setThresholdExceeded(false);
                }
            }
        });
    }


    // ******************** Public methods ************************************
    public final double getValue() {
        return value.get();
    }
    public final void setValue(final double VALUE) {
        value.set(clamp(getMinValue(), getMaxValue(), VALUE));
    }
    public final DoubleProperty valueProperty() {
        return value;
    }

    public final double getMinValue() {
        return minValue.get();
    }
    public final void setMinValue(final double MIN_VALUE) {
        minValue.set(clamp(Double.NEGATIVE_INFINITY, getMaxValue() - 1, MIN_VALUE));
        recalculate();
        updateTickLabels();
    }
    public final DoubleProperty minValueProperty() {
        return minValue;
    }

    public final double getMaxValue() {
        return maxValue.get();
    }
    public final void setMaxValue(final double MAX_VALUE) {
        maxValue.set(clamp(getMinValue() + 1, Double.POSITIVE_INFINITY, MAX_VALUE));
        recalculate();
        updateTickLabels();
    }
    public final DoubleProperty maxValueProperty() {
        return maxValue;
    }

    public final double getMinMeasuredValue() {
        return minMeasuredValue.get();
    }
    public final void setMinMeasuredValue(final double MIN_MEASURED_VALUE) {
        minMeasuredValue.set(MIN_MEASURED_VALUE);
    }
    public final DoubleProperty minMeasuredValueProperty() {
        return minMeasuredValue;
    }

    public final double getMaxMeasuredValue() {
        return maxMeasuredValue.get();
    }
    public final void setMaxMeasuredValue(final double MAX_MEASURED_VALUE) {
        maxMeasuredValue.set(MAX_MEASURED_VALUE);
    }
    public final DoubleProperty maxMeasuredValueProperty() {
        return maxMeasuredValue;
    }

    public final void resetMinMeasuredValue() {
        setMinMeasuredValue(getValue());
    }
    public final void resetMaxMeasuredValue() {
        setMaxMeasuredValue(getValue());
    }
    public final void resetMinMaxMeasuredValue() {
        setMinMeasuredValue(getValue());
        setMaxMeasuredValue(getValue());
    }

    public final double getThreshold() {
        return threshold.get();
    }
    public final void setThreshold(final double THRESHOLD) {
        threshold.set(THRESHOLD);
    }
    public final DoubleProperty thresholdProperty() {
        return threshold;
    }

    public final boolean isThresholdBehaviorInverted() {
        return thresholdBehaviorInverted.get();
    }
    public final void setThresholdBehaviorInverted(final boolean THRESHOLD_BEHAVIOR_INVERTED) {
        thresholdBehaviorInverted.set(THRESHOLD_BEHAVIOR_INVERTED);
    }
    public final BooleanProperty thresholdBehaviorInvertedProperty() {
        return thresholdBehaviorInverted;
    }

    public final boolean isThresholdExceeded() {
        return thresholdExceeded.get();
    }
    public final void setThresholdExceeded(final boolean THRESHOLD_EXCEEDED) {
        thresholdExceeded.set(THRESHOLD_EXCEEDED);
    }
    public final BooleanProperty thresholdExceededProperty() {
        return thresholdExceeded;
    }

    public final boolean isAnimated() {
        return animated.get();
    }
    public final void setAnimated(final boolean ANIMATED) {
        animated.set(ANIMATED);
    }
    public final BooleanProperty animatedProperty() {
        return animated;
    }

    public final boolean isBackgroundVisible() {
        return backgroundVisible.get();
    }
    public final void setBackgroundVisible(final boolean BACKGROUND_VISIBLE) {
        backgroundVisible.set(BACKGROUND_VISIBLE);
    }
    public final BooleanProperty backgroundVisibleProperty() {
        return backgroundVisible;
    }

    public final String getUnitString(){
        return unitString.get();
    }
    public final void setUnitString(final String UNIT_STRING) {
        unitString.set(UNIT_STRING);
    }
    public final StringProperty unitStringProperty() {
        return unitString;
    }

    public final ObservableList<Section> getSections() {
        return sections;
    }
    public final void setSections(final Section... SECTION_ARRAY) {
        sections.setAll(SECTION_ARRAY);
        addSections();
    }
    public final void setSections(final List<Section> SECTIONS) {
        sections.setAll(SECTIONS);
        addSections();
    }

    @Override protected double computePrefWidth(final double PREF_HEIGHT) {
        double prefHeight = DEFAULT_HEIGHT;
        if (PREF_HEIGHT != -1) {
            prefHeight = Math.max(0, PREF_HEIGHT - getInsets().getTop() - getInsets().getBottom());
        }
        return super.computePrefWidth(prefHeight);
    }
    @Override protected double computePrefHeight(final double PREF_WIDTH) {
        double prefWidth = DEFAULT_WIDTH;
        if (PREF_WIDTH != -1) {
            prefWidth = Math.max(0, PREF_WIDTH - getInsets().getLeft() - getInsets().getRight());
        }
        return super.computePrefWidth(prefWidth);
    }

    @Override protected double computeMinWidth(final double MIN_HEIGHT) {
        return super.computeMinWidth(Math.max(MINIMUM_HEIGHT, MIN_HEIGHT - getInsets().getTop() - getInsets().getBottom()));
    }
    @Override protected double computeMinHeight(final double MIN_WIDTH) {
        return super.computeMinHeight(Math.max(MINIMUM_WIDTH, MIN_WIDTH - getInsets().getLeft() - getInsets().getRight()));
    }

    @Override protected double computeMaxWidth(final double MAX_HEIGHT) {
        return super.computeMaxWidth(Math.min(MAXIMUM_HEIGHT, MAX_HEIGHT - getInsets().getTop() - getInsets().getBottom()));
    }
    @Override protected double computeMaxHeight(final double MAX_WIDTH) {
        return super.computeMaxHeight(Math.min(MAXIMUM_WIDTH, MAX_WIDTH - getInsets().getLeft() - getInsets().getRight()));
    }


    // ******************** Private methods ***********************************
    private void rotateNeedle() {
        valueText.setX((width - valueText.getLayoutBounds().getWidth()) * 0.5);
        valueText.setY(size * 0.85);

        double targetAngle = (getValue() - getMinValue()) * angleStep - ROTATION_OFFSET;
        if (isAnimated()) {
            needle.setCache(true);
            needle.setCacheHint(CacheHint.ROTATE);
            timeline.stop();
            final KeyValue KEY_VALUE = new KeyValue(needleRotate.angleProperty(), targetAngle, Interpolator.SPLINE(0.5, 0.4, 0.4, 1.0));
            final KeyFrame KEY_FRAME = new KeyFrame(Duration.millis(TIME_TO_VALUE), KEY_VALUE);
            timeline.getKeyFrames().setAll(KEY_FRAME);
            timeline.getKeyFrames().add(KEY_FRAME);
            timeline.play();
            timeline.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    needle.setCache(false);
                }
            });
        } else {
            needleRotate.setAngle(targetAngle);
        }
    }

    private void recalculate() {
        angleStep = ANGLE_RANGE / (getMaxValue() - getMinValue());
        setThreshold(clamp(getMinValue(), getMaxValue(), getThreshold()));
    }

    private void addSections() {
        sectionGroup.getChildren().clear();
        for (final Section section : getSections()) {
            sectionGroup.getChildren().add(section.getArea());
        }
        updateSections();
    }

    private void updateSections() {
        final double OUTER_RADIUS = size * 0.41;
        final double CENTER_X     = width * 0.5;
        final double CENTER_Y     = height * 0.5;

        for (final Section section : getSections()) {
            final double SECTION_START = clamp(getMinValue(), getMaxValue(), section.getStart());
            final double SECTION_STOP  = clamp(getMinValue(), getMaxValue(), section.getStop());
            final double ANGLE_START   = ROTATION_OFFSET - (SECTION_START * angleStep) + (getMinValue() * angleStep) + 90;
            final double ANGLE_EXTEND  = -(SECTION_STOP - SECTION_START) * angleStep;

            section.getArea().setType(ArcType.ROUND);
            section.getArea().setCenterX(CENTER_X);
            section.getArea().setCenterY(CENTER_Y);
            section.getArea().setRadiusX(OUTER_RADIUS);
            section.getArea().setRadiusY(OUTER_RADIUS);
            section.getArea().setStartAngle(ANGLE_START);
            section.getArea().setLength(ANGLE_EXTEND);
            section.getArea().setFill(section.getColor());
        }
    }

    private void updateTickLabels() {
        tickLabelGroup.getChildren().clear();
        tickLabels.clear();
        for (double i = getMinValue() ; Double.compare(i, getMaxValue()) <= 0 ; i += FRACTION) {
            Text tickLabel = TextBuilder.create()
                                        .font(tickLabelFont)
                                        .text(String.format("%.0f", i))
                                        .effect(textDropShadow)
                                        .styleClass("tick-label")
                                        .build();
            tickLabels.add(tickLabel);
        }
        tickLabelGroup.getChildren().setAll(tickLabels);
        resize();
    }

    private double clamp(final double MIN, final double MAX, final double VALUE) {
        if (VALUE < MIN) return MIN;
        if (VALUE > MAX) return MAX;
        return VALUE;
    }

    public final void drawAlertIndicator(final double WIDTH, final double HEIGHT, final Color COLOR) {
        alertIndicator.setWidth(WIDTH);
        alertIndicator.setHeight(HEIGHT);
        final GraphicsContext CTX = alertIndicator.getGraphicsContext2D();
        CTX.clearRect(0, 0, WIDTH, HEIGHT);

        //alert
        CTX.save();
        CTX.beginPath();
        CTX.moveTo(0.45161290322580644 * WIDTH, 0.8148148148148148 * HEIGHT);
        CTX.bezierCurveTo(0.45161290322580644 * WIDTH, 0.7777777777777778 * HEIGHT, 0.4838709677419355 * WIDTH, 0.7407407407407407 * HEIGHT, 0.5161290322580645 * WIDTH, 0.7407407407407407 * HEIGHT);
        CTX.bezierCurveTo(0.5161290322580645 * WIDTH, 0.7407407407407407 * HEIGHT, 0.5483870967741935 * WIDTH, 0.7777777777777778 * HEIGHT, 0.5483870967741935 * WIDTH, 0.8148148148148148 * HEIGHT);
        CTX.bezierCurveTo(0.5483870967741935 * WIDTH, 0.8148148148148148 * HEIGHT, 0.5161290322580645 * WIDTH, 0.8518518518518519 * HEIGHT, 0.5161290322580645 * WIDTH, 0.8518518518518519 * HEIGHT);
        CTX.bezierCurveTo(0.4838709677419355 * WIDTH, 0.8518518518518519 * HEIGHT, 0.45161290322580644 * WIDTH, 0.8148148148148148 * HEIGHT, 0.45161290322580644 * WIDTH, 0.8148148148148148 * HEIGHT);
        CTX.closePath();
        CTX.moveTo(0.45161290322580644 * WIDTH, 0.37037037037037035 * HEIGHT);
        CTX.bezierCurveTo(0.45161290322580644 * WIDTH, 0.3333333333333333 * HEIGHT, 0.4838709677419355 * WIDTH, 0.3333333333333333 * HEIGHT, 0.5161290322580645 * WIDTH, 0.3333333333333333 * HEIGHT);
        CTX.bezierCurveTo(0.5161290322580645 * WIDTH, 0.3333333333333333 * HEIGHT, 0.5483870967741935 * WIDTH, 0.3333333333333333 * HEIGHT, 0.5483870967741935 * WIDTH, 0.37037037037037035 * HEIGHT);
        CTX.bezierCurveTo(0.5483870967741935 * WIDTH, 0.37037037037037035 * HEIGHT, 0.5483870967741935 * WIDTH, 0.6296296296296297 * HEIGHT, 0.5483870967741935 * WIDTH, 0.6296296296296297 * HEIGHT);
        CTX.bezierCurveTo(0.5483870967741935 * WIDTH, 0.6666666666666666 * HEIGHT, 0.5161290322580645 * WIDTH, 0.7037037037037037 * HEIGHT, 0.5161290322580645 * WIDTH, 0.7037037037037037 * HEIGHT);
        CTX.bezierCurveTo(0.4838709677419355 * WIDTH, 0.7037037037037037 * HEIGHT, 0.45161290322580644 * WIDTH, 0.6666666666666666 * HEIGHT, 0.45161290322580644 * WIDTH, 0.6296296296296297 * HEIGHT);
        CTX.bezierCurveTo(0.45161290322580644 * WIDTH, 0.6296296296296297 * HEIGHT, 0.45161290322580644 * WIDTH, 0.37037037037037035 * HEIGHT, 0.45161290322580644 * WIDTH, 0.37037037037037035 * HEIGHT);
        CTX.closePath();
        CTX.moveTo(0.3225806451612903 * WIDTH, 0.9629629629629629 * HEIGHT);
        CTX.lineTo(0.6451612903225806 * WIDTH, 0.9629629629629629 * HEIGHT);
        CTX.bezierCurveTo(0.6451612903225806 * WIDTH, 0.9629629629629629 * HEIGHT, 0.8387096774193549 * WIDTH, 0.9629629629629629 * HEIGHT, 0.8387096774193549 * WIDTH, 0.9629629629629629 * HEIGHT);
        CTX.bezierCurveTo(0.9354838709677419 * WIDTH, 0.9629629629629629 * HEIGHT, 0.967741935483871 * WIDTH, 0.8888888888888888 * HEIGHT, 0.9032258064516129 * WIDTH, 0.8148148148148148 * HEIGHT);
        CTX.bezierCurveTo(0.9032258064516129 * WIDTH, 0.8148148148148148 * HEIGHT, 0.5806451612903226 * WIDTH, 0.1111111111111111 * HEIGHT, 0.5806451612903226 * WIDTH, 0.1111111111111111 * HEIGHT);
        CTX.bezierCurveTo(0.5161290322580645 * WIDTH, 0.037037037037037035 * HEIGHT, 0.45161290322580644 * WIDTH, 0.037037037037037035 * HEIGHT, 0.41935483870967744 * WIDTH, 0.1111111111111111 * HEIGHT);
        CTX.bezierCurveTo(0.41935483870967744 * WIDTH, 0.1111111111111111 * HEIGHT, 0.06451612903225806 * WIDTH, 0.8148148148148148 * HEIGHT, 0.06451612903225806 * WIDTH, 0.8148148148148148 * HEIGHT);
        CTX.bezierCurveTo(0.03225806451612903 * WIDTH, 0.8888888888888888 * HEIGHT, 0.06451612903225806 * WIDTH, 0.9629629629629629 * HEIGHT, 0.16129032258064516 * WIDTH, 0.9629629629629629 * HEIGHT);
        CTX.bezierCurveTo(0.16129032258064516 * WIDTH, 0.9629629629629629 * HEIGHT, 0.3225806451612903 * WIDTH, 0.9629629629629629 * HEIGHT, 0.3225806451612903 * WIDTH, 0.9629629629629629 * HEIGHT);
        CTX.closePath();
        CTX.setFill(COLOR);
        CTX.fill();
        CTX.restore();
    }

    private void resize() {
        size   = getWidth() < getHeight() ? getWidth() : getHeight();
        width  = getWidth();
        height = getHeight();

        if (aspectRatio * width > height) {
            width  = 1 / (aspectRatio / height);
        } else if (1 / (aspectRatio / height) > width) {
            height = aspectRatio * width;
        }

        frame.setPrefSize(1.0 * width, 1.0 * height);
        frame.setTranslateX(0.0 * width);
        frame.setTranslateY(0.0 * height);

        background.setPrefSize(0.955 * width, 0.955 * height);
        background.setTranslateX(0.0225 * width);
        background.setTranslateY(0.0225 * height);

        updateSections();

        textDropShadow.setRadius(size * 0.005);
        textDropShadow.setOffsetY(size * 0.0025);

        tickLabelFont = Font.font("Verdana", FontWeight.BOLD, size * 0.04);
        int tickLabelCounter = 0;
        for (double angle = -30 ; Double.compare(angle, -ANGLE_RANGE - 30) >= 0 ; angle -= (FRACTION * angleStep)) {
            double x = 0.31 * size * Math.sin(Math.toRadians(angle));
            double y = 0.31 * size * Math.cos(Math.toRadians(angle));
            tickLabels.get(tickLabelCounter).setFont(tickLabelFont);
            tickLabels.get(tickLabelCounter).setX(size * 0.5 + x - tickLabels.get(tickLabelCounter).getLayoutBounds().getWidth() * 0.5);
            tickLabels.get(tickLabelCounter).setY(size * 0.5 + y);
            tickLabels.get(tickLabelCounter).setTextOrigin(VPos.CENTER);
            tickLabels.get(tickLabelCounter).setTextAlignment(TextAlignment.CENTER);
            tickLabelCounter++;
        }

        pointerShadow.setRadius(0.05 * size);
        pointerShadow.setOffsetY(0.02 * size);

        needle.setPrefSize(0.3234653091430664 * width, 0.4225 * height);
        needle.setTranslateX(0.33826732635498047 * width);
        needle.setTranslateY(0.07625 * height);
        needleRotate.setPivotX((needle.getPrefWidth()) * 0.5);
        needleRotate.setPivotY(needle.getPrefHeight());

        knob.setPrefSize(0.295 * width, 0.295 * height);
        knob.setTranslateX(0.3525 * width);
        knob.setTranslateY(0.345 * height);
        gaugeKnobInnerShadow0.setRadius(0.005 * size);
        gaugeKnobInnerShadow1.setRadius(0.005 * size);
        gaugeKnobDropShadow.setRadius(0.05 * size);
        gaugeKnobDropShadow.setOffsetY(0.02 * size);

        unitFont = Font.font("Verdana", FontWeight.NORMAL, size * 0.16);
        unitText.setFont(unitFont);
        unitText.setX((width - unitText.getLayoutBounds().getWidth()) * 0.5);
        unitText.setY((height - unitText.getLayoutBounds().getHeight()) * 0.5 + unitText.getLayoutBounds().getHeight() * 0.5);

        valueFont = Font.font("Verdana", FontWeight.NORMAL, size * 0.07);
        valueText.setFont(valueFont);
        valueText.setX((width - valueText.getLayoutBounds().getWidth()) * 0.5);
        valueText.setY(size * 0.85);

        drawAlertIndicator(0.124 * size, 0.108 * size, Color.RED);
        alertIndicator.setTranslateX((width - alertIndicator.getLayoutBounds().getWidth()) * 0.5);
        alertIndicator.setTranslateY(height * 0.68);
    }
}
