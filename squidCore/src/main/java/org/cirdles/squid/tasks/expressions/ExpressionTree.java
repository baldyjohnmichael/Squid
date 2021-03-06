/* 
 * Copyright 2006-2017 CIRDLES.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cirdles.squid.tasks.expressions;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.List;
import org.cirdles.squid.exceptions.SquidException;
import org.cirdles.squid.projects.SquidProject;
import org.cirdles.squid.shrimp.ShrimpFractionExpressionInterface;
import org.cirdles.squid.utilities.xmlSerialization.XMLSerializerInterface;
import org.cirdles.squid.tasks.TaskInterface;
import org.cirdles.squid.tasks.expressions.builtinExpressions.SquidExpressionMinus1;
import org.cirdles.squid.tasks.expressions.constants.ConstantNode;
import org.cirdles.squid.tasks.expressions.constants.ConstantNodeXMLConverter;
import org.cirdles.squid.tasks.expressions.functions.Function;
import org.cirdles.squid.tasks.expressions.functions.FunctionXMLConverter;
import org.cirdles.squid.tasks.expressions.isotopes.ShrimpSpeciesNode;
import org.cirdles.squid.tasks.expressions.isotopes.ShrimpSpeciesNodeXMLConverter;
import org.cirdles.squid.tasks.expressions.operations.Operation;
import org.cirdles.squid.tasks.expressions.operations.OperationXMLConverter;
import org.cirdles.squid.tasks.expressions.variables.VariableNodeForPerSpotTaskExpressions;
import org.cirdles.squid.tasks.expressions.variables.VariableNodeForIsotopicRatios;
import org.cirdles.squid.tasks.expressions.variables.VariableNodeForSummary;
import org.cirdles.squid.tasks.expressions.variables.VariableXMLConverter;

/**
 *
 * @author James F. Bowring
 */
public class ExpressionTree
        implements ExpressionTreeInterface,
        ExpressionTreeBuilderInterface,
        ExpressionTreeWithRatiosInterface,
        XMLSerializerInterface {

    public static SquidProject squidProject;
    /**
     *
     */
    protected String name;

    /**
     *
     */
    private List<ExpressionTreeInterface> childrenET;

    /**
     *
     */
    protected ExpressionTreeInterface parentET;

    /**
     *
     */
    protected OperationOrFunctionInterface operation;

    /**
     *
     */
    protected List<String> ratiosOfInterest;

    /**
     *
     */
    protected boolean squidSwitchSCSummaryCalculation;

    /**
     *
     */
    protected boolean squidSwitchSTReferenceMaterialCalculation;

    /**
     *
     */
    protected boolean squidSwitchSAUnknownCalculation;

    /**
     *
     */
    protected boolean rootExpressionTree;

    /**
     *
     */
    public ExpressionTree() {
        this("Anonymous");
    }

    /**
     *
     * @param prettyName the value of prettyName
     */
    public ExpressionTree(String prettyName) {
        this(prettyName, null, null, null);
    }

    /**
     *
     * @param operation
     */
    public ExpressionTree(OperationOrFunctionInterface operation) {
        this();
        this.operation = operation;
    }

    /**
     *
     * @param prettyName the value of name
     * @param leftET the value of leftET
     * @param rightET the value of rightET
     * @param operation the value of operation
     */
    public ExpressionTree(String prettyName, ExpressionTreeInterface leftET, ExpressionTreeInterface rightET, OperationOrFunctionInterface operation) {
        this(prettyName, leftET, rightET, operation, new ArrayList<String>());
    }

    /**
     *
     * @param prettyName the value of prettyName
     * @param leftET the value of leftET
     * @param rightET the value of rightET
     * @param operation the value of operation
     * @param ratiosOfInterest the value of ratiosOfInterest
     */
    private ExpressionTree(String prettyName, ExpressionTreeInterface leftET, ExpressionTreeInterface rightET, OperationOrFunctionInterface operation, List<String> ratiosOfInterest) {
        this.name = prettyName;
        this.childrenET = new ArrayList<>();
        populateChildrenET(leftET, rightET);
        this.parentET = null;
        this.operation = operation;
        this.ratiosOfInterest = ratiosOfInterest;
        this.squidSwitchSCSummaryCalculation = false;
        this.squidSwitchSTReferenceMaterialCalculation = false;
        this.squidSwitchSAUnknownCalculation = false;
        this.rootExpressionTree = false;
    }

    private void populateChildrenET(ExpressionTreeInterface leftET, ExpressionTreeInterface rightET) {
        addChild(leftET);
        addChild(rightET);
    }

    /**
     *
     * @param xstream
     */
    @Override
    public void customizeXstream(XStream xstream) {
        xstream.registerConverter(new ShrimpSpeciesNodeXMLConverter());
        xstream.alias("ShrimpSpeciesNode", ShrimpSpeciesNode.class);

        xstream.registerConverter(new ConstantNodeXMLConverter());
        xstream.alias("ConstantNode", ConstantNode.class);

        xstream.registerConverter(new VariableXMLConverter());
        xstream.alias("VariableNodeForSummary", VariableNodeForSummary.class);
        xstream.alias("VariableNodeForPerSpotTaskExpressions", VariableNodeForPerSpotTaskExpressions.class);
        xstream.alias("VariableNodeForIsotopicRatios", VariableNodeForIsotopicRatios.class);

        xstream.registerConverter(new OperationXMLConverter());
        xstream.registerConverter(new FunctionXMLConverter());

        xstream.registerConverter(new ExpressionTreeXMLConverter());
        xstream.alias("ExpressionTree", ExpressionTree.class);

        // Note: http://cristian.sulea.net/blog.php?p=2014-11-12-xstream-object-references
        xstream.setMode(XStream.NO_REFERENCES);

    }

    /**
     *
     * @param xml
     * @return
     */
    @Override
    public String customizeXML(String xml) {
        String xmlR = xml;

        // TODO: Move to global once we decide where this puppy will live
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<ExpressionTree xmlns=\"https://raw.githubusercontent.com\"\n"
                + " xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + " xsi:schemaLocation=\"https://raw.githubusercontent.com\n"
                + "                 https://raw.githubusercontent.com/CIRDLES/Calamari/master/src/main/resources/SquidExpressionModelXMLSchema.xsd\">";

        xmlR = xmlR.replaceFirst("<ExpressionTree>",
                header);

        return xmlR;
    }

    /**
     *
     * @param shrimpFractions the value of shrimpFraction
     * @param task
     * @return the double[][]
     */
    @Override
    public Object[][] eval(List<ShrimpFractionExpressionInterface> shrimpFractions, TaskInterface task) throws SquidException {
        return operation == null ? new Object[][]{{0.0, 0.0}} : operation.eval(childrenET, shrimpFractions, task);
    }

    /**
     *
     * @return
     */
    @Override
    public int getOperationPrecedence() {
        int retVal = 100;

        if (operation != null) {
            retVal = operation.getPrecedence();
        }

        return retVal;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isTypeFunction() {
        return (operation instanceof Function);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isTypeFunctionOrOperation() {
        return (operation instanceof Function) || (operation instanceof Operation);
    }

    /**
     *
     * @return
     */
    @Override
    public int argumentCount() {
        return operation.getArgumentCount();
    }

    /**
     *
     * @return
     */
    @Override
    public String toStringMathML() {
        String retVal = "";
        if (operation == null) {
            retVal = "<mtext>No expression selected.</mtext>\n";
        } else {
            retVal = operation.toStringMathML(childrenET);
        }
        return retVal;
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the childrenET
     */
    public List<ExpressionTreeInterface> getChildrenET() {
        return childrenET;
    }

    /**
     * @param childrenET the childrenET to set
     */
    public void setChildrenET(List<ExpressionTreeInterface> childrenET) {
        this.childrenET = childrenET;
    }

    /**
     * @return the parentET
     */
    @Override
    public ExpressionTreeInterface getParentET() {
        return parentET;
    }

    /**
     * @param parentET the parentET to set
     */
    @Override
    public void setParentET(ExpressionTreeInterface parentET) {
        this.parentET = parentET;
    }

    /**
     * @return the leftET
     */
    @Override
    public ExpressionTreeInterface getLeftET() {
        ExpressionTreeInterface retVal = null;
        try {
            retVal = childrenET.get(0);
        } catch (Exception e) {
        }
        return retVal;
    }

    /**
     * @return the rightET
     */
    @Override
    public ExpressionTreeInterface getRightET() {
        ExpressionTreeInterface retVal = null;
        try {
            retVal = childrenET.get(1);
        } catch (Exception e) {
        }
        return retVal;
    }

    /**
     *
     * @return
     */
    @Override
    public int getCountOfChildren() {
        return childrenET.size();// - (int) ((leftET == null) ? 1 : 0);
    }

    /**
     *
     * @param childET
     */
    @Override
    public void addChild(ExpressionTreeInterface childET) {
        if (childET != null) {
            childrenET.add(childET);
            childET.setParentET(this);
        }
    }

    /**
     *
     * @param index
     * @param childET
     */
    @Override
    public void addChild(int index, ExpressionTreeInterface childET) {
        if (childET != null) {
            childrenET.add(index, childET);
            childET.setParentET(this);
        }
    }

    /**
     * @return the operation
     */
    @Override
    public OperationOrFunctionInterface getOperation() {
        return operation;
    }

    /**
     * @param operation the operation to set
     */
    @Override
    public void setOperation(OperationOrFunctionInterface operation) {
        this.operation = operation;
    }

    /**
     * @return the ratiosOfInterest
     */
    @Override
    public List<String> getRatiosOfInterest() {
        return ratiosOfInterest;
    }

    /**
     * @param ratiosOfInterest the ratiosOfInterest to set
     */
    public void setRatiosOfInterest(List<String> ratiosOfInterest) {
        this.ratiosOfInterest = ratiosOfInterest;
    }

    /**
     *
     * @return
     */
    public boolean hasRatiosOfInterest() {
        return ratiosOfInterest.size() > 0;
    }

    /**
     * @return the rootExpressionTree
     */
    @Override
    public boolean isRootExpressionTree() {
        return rootExpressionTree;
    }

    /**
     * @param rootExpressionTree the rootExpressionTree to set
     */
    public void setRootExpressionTree(boolean rootExpressionTree) {
        this.rootExpressionTree = rootExpressionTree;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * @return the squidSwitchSCSummaryCalculation
     */
    public boolean isSquidSwitchSCSummaryCalculation() {
        return squidSwitchSCSummaryCalculation;
    }

    /**
     * @param squidSwitchSCSummaryCalculation the
     * squidSwitchSCSummaryCalculation to set
     */
    public void setSquidSwitchSCSummaryCalculation(boolean squidSwitchSCSummaryCalculation) {
        this.squidSwitchSCSummaryCalculation = squidSwitchSCSummaryCalculation;
    }

    /**
     * @return the squidSwitchSTReferenceMaterialCalculation
     */
    public boolean isSquidSwitchSTReferenceMaterialCalculation() {
        return squidSwitchSTReferenceMaterialCalculation;
    }

    /**
     * @param squidSwitchSTReferenceMaterialCalculation the
     * squidSwitchSTReferenceMaterialCalculation to set
     */
    public void setSquidSwitchSTReferenceMaterialCalculation(boolean squidSwitchSTReferenceMaterialCalculation) {
        this.squidSwitchSTReferenceMaterialCalculation = squidSwitchSTReferenceMaterialCalculation;
    }

    /**
     * @return the squidSwitchSAUnknownCalculation
     */
    public boolean isSquidSwitchSAUnknownCalculation() {
        return squidSwitchSAUnknownCalculation;
    }

    /**
     * @param squidSwitchSAUnknownCalculation the
     * squidSwitchSAUnknownCalculation to set
     */
    public void setSquidSwitchSAUnknownCalculation(boolean squidSwitchSAUnknownCalculation) {
        this.squidSwitchSAUnknownCalculation = squidSwitchSAUnknownCalculation;
    }

    public static void main(String[] args) {
//        ExpressionTreeInterface EXPRESSION = new ExpressionTree("Net204cts/sec");
//
//        ((ExpressionTreeBuilderInterface) EXPRESSION).addChild(0, new ShrimpSpeciesNode(new SquidSpeciesModel(-1, "DummyMass", "204", "Pb", false), "getTotalCps"));
//        ((ExpressionTreeBuilderInterface) EXPRESSION).addChild(new ShrimpSpeciesNode(new SquidSpeciesModel(-1, "BKG", "204", "Pb", true), "getTotalCps"));
//        ((ExpressionTreeBuilderInterface) EXPRESSION).setOperation(Operation.subtract());
//
//        ((ExpressionTree) EXPRESSION).setRootExpressionTree(true);
//        ((ExpressionTree) EXPRESSION).setSquidSwitchSCSummaryCalculation(false);
//        ((ExpressionTree) EXPRESSION).setSquidSwitchSTReferenceMaterialCalculation(true);
//        ((ExpressionTree) EXPRESSION).setSquidSwitchSAUnknownCalculation(false);
//        

        ExpressionTreeInterface EXPRESSION = new ExpressionTree("206/238 Calib Const");

//        ((ExpressionTreeWithRatiosInterface) EXPRESSION).getRatiosOfInterest().add("206/238");
//        ((ExpressionTreeWithRatiosInterface) EXPRESSION).getRatiosOfInterest().add("254/238");
//
//        ExpressionTreeInterface r254_238wSquared = new ExpressionTree("254/238^2", new ExpressionTree(
//                            "254/238",
//                            new ShrimpSpeciesNode(new SquidSpeciesModel(-1, "254Mass", "254", "Pb", false), "getPkInterpScanArray"),
//                            new ShrimpSpeciesNode(new SquidSpeciesModel(-1, "238Mass", "238", "U", false), "getPkInterpScanArray"),
//                            Operation.divide()), new ConstantNode("2", 2.0), Operation.pow());
//
//        ((ExpressionTreeBuilderInterface) EXPRESSION).addChild(0, new ExpressionTree(
//                            "254/238",
//                            new ShrimpSpeciesNode(new SquidSpeciesModel(-1, "206Mass", "206", "Pb", false), "getPkInterpScanArray"),
//                            new ShrimpSpeciesNode(new SquidSpeciesModel(-1, "238Mass", "238", "U", false), "getPkInterpScanArray"),
//                            Operation.divide()));
//        
//        ((ExpressionTreeBuilderInterface) EXPRESSION).addChild(r254_238wSquared);
//        ((ExpressionTreeBuilderInterface) EXPRESSION).setOperation(Operation.divide());
//
//        ((ExpressionTree) EXPRESSION).setRootExpressionTree(true);
//        ((ExpressionTree) EXPRESSION).setSquidSwitchSCSummaryCalculation(false);
//        ((ExpressionTree) EXPRESSION).setSquidSwitchSTReferenceMaterialCalculation(true);
//        ((ExpressionTree) EXPRESSION).setSquidSwitchSAUnknownCalculation(true);
        squidProject = new SquidProject();
        ((XMLSerializerInterface) EXPRESSION).serializeXMLObject(SquidExpressionMinus1.EXPRESSION, "XXXExpressionTree.xml");
    }
}
