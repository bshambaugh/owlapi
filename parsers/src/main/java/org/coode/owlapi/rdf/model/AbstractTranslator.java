/*
 * This file is part of the OWL API.
 *
 * The contents of this file are subject to the LGPL License, Version 3.0.
 *
 * Copyright (C) 2011, The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0
 * in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 *
 * Copyright 2011, University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.coode.owlapi.rdf.model;

import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.*;
import static org.semanticweb.owlapi.vocab.SWRLVocabulary.*;

import java.util.*;

import org.semanticweb.owlapi.io.RDFOntologyFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.IndividualAppearance;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

/**
 * An abstract translator that can produce an RDF graph from an OWLOntology.
 * Subclasses must provide implementations to create concrete representations of
 * resources, triples etc.
 * 
 * @param <NODE>
 *        the basic node
 * @param <RESOURCE>
 *        a resource node
 * @param <PREDICATE>
 *        a predicate node
 * @param <LITERAL>
 *        a literal node
 * @author Matthew Horridge, The University Of Manchester, Bio-Health
 *         Informatics Group, Date: 06-Dec-2006
 */
public abstract class AbstractTranslator<NODE, RESOURCE extends NODE, PREDICATE extends NODE, LITERAL extends NODE>
    implements OWLObjectVisitor, SWRLObjectVisitor {

    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private boolean useStrongTyping = true;
    protected final IndividualAppearance multipleOccurrences;

    /**
     * @param manager
     *        the manager
     * @param ontology
     *        the ontology
     * @param useStrongTyping
     *        true if strong typing should be used
     */
    public AbstractTranslator(OWLOntologyManager manager, OWLOntology ontology,
        boolean useStrongTyping, IndividualAppearance multiple) {
        this.ontology = ontology;
        this.manager = manager;
        this.useStrongTyping = useStrongTyping;
        multipleOccurrences = multiple;
    }

    @Override
    public void visit(OWLDeclarationAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getEntity(), RDF_TYPE.getIRI(), axiom
            .getEntity().accept(OWLEntityTypeProvider.INSTANCE));
    }

    @Override
    public void visit(OWLObjectInverseOf property) {
        translateAnonymousNode(property);
        addTriple(property, OWL_INVERSE_OF.getIRI(), property.getInverse());
    }

    @Override
    public void visit(OWLDataIntersectionOf node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), RDFS_DATATYPE.getIRI());
        addListTriples(node, OWL_INTERSECTION_OF.getIRI(), node.getOperands());
    }

    @Override
    public void visit(OWLDataUnionOf node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), RDFS_DATATYPE.getIRI());
        addListTriples(node, OWL_UNION_OF.getIRI(), node.getOperands());
    }

    @Override
    public void visit(OWLDataComplementOf node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), RDFS_DATATYPE.getIRI());
        addTriple(node, OWL_DATATYPE_COMPLEMENT_OF.getIRI(),
            node.getDataRange());
    }

    @Override
    public void visit(OWLDataOneOf node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), RDFS_DATATYPE.getIRI());
        addListTriples(node, OWL_ONE_OF.getIRI(), node.getValues());
    }

    @Override
    public void visit(OWLDatatypeRestriction node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), RDFS_DATATYPE.getIRI());
        addTriple(node, OWL_ON_DATA_TYPE.getIRI(), node.getDatatype());
        addListTriples(node, OWL_WITH_RESTRICTIONS.getIRI(),
            node.getFacetRestrictions());
    }

    @Override
    public void visit(OWLObjectIntersectionOf desc) {
        translateAnonymousNode(desc);
        addListTriples(desc, OWL_INTERSECTION_OF.getIRI(), desc.getOperands());
        addTriple(desc, RDF_TYPE.getIRI(), OWL_CLASS.getIRI());
    }

    @Override
    public void visit(OWLObjectUnionOf desc) {
        translateAnonymousNode(desc);
        addTriple(desc, RDF_TYPE.getIRI(), OWL_CLASS.getIRI());
        addListTriples(desc, OWL_UNION_OF.getIRI(), desc.getOperands());
    }

    @Override
    public void visit(OWLObjectComplementOf desc) {
        translateAnonymousNode(desc);
        addTriple(desc, RDF_TYPE.getIRI(), OWL_CLASS.getIRI());
        addTriple(desc, OWL_COMPLEMENT_OF.getIRI(), desc.getOperand());
    }

    @Override
    public void visit(OWLObjectOneOf desc) {
        translateAnonymousNode(desc);
        addTriple(desc, RDF_TYPE.getIRI(), OWL_CLASS.getIRI());
        addListTriples(desc, OWL_ONE_OF.getIRI(), desc.getIndividuals());
        processIfAnonymous(desc.getIndividuals(), null);
    }

    // ///////////////////////////////////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////////////////
    // //
    // // Translation of restrictions
    // //
    // //
    // ///////////////////////////////////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////////////////
    /**
     * Add type triples and the owl:onProperty triples for an OWLRestriction.
     * 
     * @param desc
     *        The restriction
     */
    private <R extends OWLPropertyRange, P extends OWLPropertyExpression<R, P>, F extends OWLPropertyRange> void addRestrictionCommonTriplePropertyRange(
        OWLRestriction<R, P, F> desc) {
        translateAnonymousNode(desc);
        addTriple(desc, RDF_TYPE.getIRI(), OWL_RESTRICTION.getIRI());
        addTriple(desc, OWL_ON_PROPERTY.getIRI(), desc.getProperty());
    }

    private <R extends OWLPropertyRange, P extends OWLPropertyExpression<R, P>, F extends OWLPropertyExpression<R, P>> void addRestrictionCommonTriplePropertyExpression(
        OWLRestriction<R, P, F> desc) {
        translateAnonymousNode(desc);
        addTriple(desc, RDF_TYPE.getIRI(), OWL_RESTRICTION.getIRI());
        addTriple(desc, OWL_ON_PROPERTY.getIRI(), desc.getProperty());
    }

    private void addObjectCardinalityRestrictionTriples(
        OWLCardinalityRestriction<OWLClassExpression, OWLObjectPropertyExpression, OWLClassExpression> ce,
        OWLRDFVocabulary cardiPredicate) {
        addRestrictionCommonTriplePropertyRange(ce);
        addTriple(ce, cardiPredicate.getIRI(),
            toTypedConstant(ce.getCardinality()));
        if (ce.isQualified()) {
            if (ce.isObjectRestriction()) {
                addTriple(ce, OWL_ON_CLASS.getIRI(), ce.getFiller());
            } else {
                addTriple(ce, OWL_ON_DATA_RANGE.getIRI(), ce.getFiller());
            }
        }
    }

    private void addDataCardinalityRestrictionTriples(
        OWLCardinalityRestriction<OWLDataRange, OWLDataPropertyExpression, OWLDataRange> ce,
        OWLRDFVocabulary cardiPredicate) {
        addRestrictionCommonTriplePropertyRange(ce);
        addTriple(ce, cardiPredicate.getIRI(),
            toTypedConstant(ce.getCardinality()));
        if (ce.isQualified()) {
            if (ce.isObjectRestriction()) {
                addTriple(ce, OWL_ON_CLASS.getIRI(), ce.getFiller());
            } else {
                addTriple(ce, OWL_ON_DATA_RANGE.getIRI(), ce.getFiller());
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void visit(OWLObjectSomeValuesFrom desc) {
        addRestrictionCommonTriplePropertyRange(desc);
        addTriple(desc, OWL_SOME_VALUES_FROM.getIRI(), desc.getFiller());
    }

    @Override
    public void visit(OWLObjectAllValuesFrom desc) {
        addRestrictionCommonTriplePropertyRange(desc);
        addTriple(desc, OWL_ALL_VALUES_FROM.getIRI(), desc.getFiller());
    }

    @Override
    public void visit(OWLObjectHasValue desc) {
        addRestrictionCommonTriplePropertyExpression(desc);
        addTriple(desc, OWL_HAS_VALUE.getIRI(), desc.getValue());
        processIfAnonymous(desc.getValue(), null);
    }

    @Override
    public void visit(OWLObjectHasSelf desc) {
        translateAnonymousNode(desc);
        addTriple(desc, RDF_TYPE.getIRI(), OWL_RESTRICTION.getIRI());
        addTriple(desc, OWL_ON_PROPERTY.getIRI(), desc.getProperty());
        addTriple(desc, OWL_HAS_SELF.getIRI(), manager.getOWLDataFactory()
            .getOWLLiteral(true));
    }

    @Override
    public void visit(OWLObjectMinCardinality desc) {
        if (desc.isQualified()) {
            addObjectCardinalityRestrictionTriples(desc,
                OWL_MIN_QUALIFIED_CARDINALITY);
        } else {
            addObjectCardinalityRestrictionTriples(desc, OWL_MIN_CARDINALITY);
        }
    }

    @Override
    public void visit(OWLObjectMaxCardinality desc) {
        if (desc.isQualified()) {
            addObjectCardinalityRestrictionTriples(desc,
                OWL_MAX_QUALIFIED_CARDINALITY);
        } else {
            addObjectCardinalityRestrictionTriples(desc, OWL_MAX_CARDINALITY);
        }
    }

    @Override
    public void visit(OWLObjectExactCardinality desc) {
        if (desc.isQualified()) {
            addObjectCardinalityRestrictionTriples(desc,
                OWL_QUALIFIED_CARDINALITY);
        } else {
            addObjectCardinalityRestrictionTriples(desc, OWL_CARDINALITY);
        }
    }

    @Override
    public void visit(OWLDataSomeValuesFrom desc) {
        addRestrictionCommonTriplePropertyRange(desc);
        addTriple(desc, OWL_SOME_VALUES_FROM.getIRI(), desc.getFiller());
    }

    @Override
    public void visit(OWLDataAllValuesFrom desc) {
        addRestrictionCommonTriplePropertyRange(desc);
        addTriple(desc, OWL_ALL_VALUES_FROM.getIRI(), desc.getFiller());
    }

    @Override
    public void visit(OWLDataHasValue desc) {
        addRestrictionCommonTriplePropertyExpression(desc);
        addTriple(desc, OWL_HAS_VALUE.getIRI(), desc.getValue());
    }

    @Override
    public void visit(OWLDataMinCardinality desc) {
        if (desc.isQualified()) {
            addDataCardinalityRestrictionTriples(desc,
                OWL_MIN_QUALIFIED_CARDINALITY);
        } else {
            addDataCardinalityRestrictionTriples(desc, OWL_MIN_CARDINALITY);
        }
    }

    @Override
    public void visit(OWLDataMaxCardinality desc) {
        if (desc.isQualified()) {
            addDataCardinalityRestrictionTriples(desc,
                OWL_MAX_QUALIFIED_CARDINALITY);
        } else {
            addDataCardinalityRestrictionTriples(desc, OWL_MAX_CARDINALITY);
        }
    }

    @Override
    public void visit(OWLDataExactCardinality desc) {
        if (desc.isQualified()) {
            addDataCardinalityRestrictionTriples(desc,
                OWL_QUALIFIED_CARDINALITY);
        } else {
            addDataCardinalityRestrictionTriples(desc, OWL_CARDINALITY);
        }
    }

    // ////////////////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////////////////
    // //
    // // Axioms
    // //
    // ////////////////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void visit(OWLSubClassOfAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getSubClass(),
            RDFS_SUBCLASS_OF.getIRI(), axiom.getSuperClass());
    }

    @Override
    public void visit(OWLEquivalentClassesAxiom axiom) {
        if (axiom.getClassExpressions().size() == 2) {
            addPairwiseClassExpressions(axiom, axiom.getClassExpressions(),
                OWL_EQUIVALENT_CLASS.getIRI());
        } else {
            for (OWLEquivalentClassesAxiom ax : splitToAnnotatedPairs(axiom)) {
                ax.accept(this);
            }
        }
    }

    private List<OWLEquivalentClassesAxiom> splitToAnnotatedPairs(OWLEquivalentClassesAxiom ax) {
        List<OWLClassExpression> individuals = new ArrayList<OWLClassExpression>(ax.getClassExpressions());
        List<OWLEquivalentClassesAxiom> result = new ArrayList<OWLEquivalentClassesAxiom>();
        for (int i = 0; i < individuals.size() - 1; i++) {
            OWLClassExpression indI = individuals.get(i);
            OWLClassExpression indJ = individuals.get(i + 1);
            result.add(manager.getOWLDataFactory().getOWLEquivalentClassesAxiom(indI, indJ, ax.getAnnotations()));
        }
        Collections.sort(result);
        return result;
    }

    private List<OWLEquivalentObjectPropertiesAxiom> splitToAnnotatedPairs(OWLEquivalentObjectPropertiesAxiom ax) {
        List<OWLObjectPropertyExpression> individuals = new ArrayList<OWLObjectPropertyExpression>(ax.getProperties());
        List<OWLEquivalentObjectPropertiesAxiom> result = new ArrayList<OWLEquivalentObjectPropertiesAxiom>();
        for (int i = 0; i < individuals.size() - 1; i++) {
            OWLObjectPropertyExpression indI = individuals.get(i);
            OWLObjectPropertyExpression indJ = individuals.get(i + 1);
            result.add(manager.getOWLDataFactory().getOWLEquivalentObjectPropertiesAxiom(indI, indJ, ax
                .getAnnotations()));
        }
        Collections.sort(result);
        return result;
    }

    private List<OWLEquivalentDataPropertiesAxiom> splitToAnnotatedPairs(OWLEquivalentDataPropertiesAxiom ax) {
        List<OWLDataPropertyExpression> individuals = new ArrayList<OWLDataPropertyExpression>(ax.getProperties());
        List<OWLEquivalentDataPropertiesAxiom> result = new ArrayList<OWLEquivalentDataPropertiesAxiom>();
        for (int i = 0; i < individuals.size() - 1; i++) {
            OWLDataPropertyExpression indI = individuals.get(i);
            OWLDataPropertyExpression indJ = individuals.get(i + 1);
            result.add(manager.getOWLDataFactory().getOWLEquivalentDataPropertiesAxiom(indI, indJ, ax
                .getAnnotations()));
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public void visit(OWLDisjointClassesAxiom axiom) {
        if (axiom.getClassExpressions().size() == 2) {
            addPairwiseClassExpressions(axiom, axiom.getClassExpressions(),
                OWL_DISJOINT_WITH.getIRI());
        } else {
            translateAnonymousNode(axiom);
            addTriple(axiom, RDF_TYPE.getIRI(),
                OWL_ALL_DISJOINT_CLASSES.getIRI());
            addListTriples(axiom, OWL_MEMBERS.getIRI(),
                axiom.getClassExpressions());
            translateAnnotations(axiom);
        }
    }

    @Override
    public void visit(OWLDisjointUnionAxiom axiom) {
        List<OWLClassExpression> classExpressions = new ArrayList<OWLClassExpression>(axiom.getClassExpressions());
        Collections.sort(classExpressions);
        addSingleTripleAxiom(axiom, axiom.getOWLClass(),
            OWL_DISJOINT_UNION_OF.getIRI(), classExpressions);
    }

    @Override
    public void visit(OWLSubObjectPropertyOfAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getSubProperty(),
            RDFS_SUB_PROPERTY_OF.getIRI(), axiom.getSuperProperty());
    }

    @Override
    public void visit(OWLSubPropertyChainOfAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getSuperProperty(),
            OWL_PROPERTY_CHAIN_AXIOM.getIRI(), axiom.getPropertyChain());
    }

    @Override
    public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
        if (axiom.getProperties().size() == 2) {
            addPairwise(axiom, axiom.getProperties(),
                OWL_EQUIVALENT_PROPERTY.getIRI());
        } else {
            for (OWLEquivalentObjectPropertiesAxiom ax : splitToAnnotatedPairs(axiom)) {
                ax.accept(this);
            }
        }
    }

    @Override
    public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
        if (axiom.getProperties().size() == 2) {
            addPairwise(axiom, axiom.getProperties(),
                OWL_PROPERTY_DISJOINT_WITH.getIRI());
        } else {
            translateAnonymousNode(axiom);
            translateAnnotations(axiom);
            addTriple(axiom, RDF_TYPE.getIRI(),
                OWL_ALL_DISJOINT_PROPERTIES.getIRI());
            addListTriples(axiom, OWL_MEMBERS.getIRI(), axiom.getProperties());
        }
    }

    @Override
    public void visit(OWLObjectPropertyDomainAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDFS_DOMAIN.getIRI(),
            axiom.getDomain());
    }

    @Override
    public void visit(OWLObjectPropertyRangeAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDFS_RANGE.getIRI(),
            axiom.getRange());
    }

    @Override
    public void visit(OWLInverseObjectPropertiesAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getFirstProperty(),
            OWL_INVERSE_OF.getIRI(), axiom.getSecondProperty());
    }

    @Override
    public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDF_TYPE.getIRI(),
            OWL_FUNCTIONAL_PROPERTY.getIRI());
    }

    @Override
    public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDF_TYPE.getIRI(),
            OWL_INVERSE_FUNCTIONAL_PROPERTY.getIRI());
    }

    @Override
    public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDF_TYPE.getIRI(),
            OWL_REFLEXIVE_PROPERTY.getIRI());
    }

    @Override
    public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDF_TYPE.getIRI(),
            OWL_IRREFLEXIVE_PROPERTY.getIRI());
    }

    @Override
    public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDF_TYPE.getIRI(),
            OWL_SYMMETRIC_PROPERTY.getIRI());
    }

    @Override
    public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDF_TYPE.getIRI(),
            OWL_ASYMMETRIC_PROPERTY.getIRI());
    }

    @Override
    public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDF_TYPE.getIRI(),
            OWL_TRANSITIVE_PROPERTY.getIRI());
    }

    @Override
    public void visit(OWLSubDataPropertyOfAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getSubProperty(),
            RDFS_SUB_PROPERTY_OF.getIRI(), axiom.getSuperProperty());
    }

    @Override
    public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
        Set<OWLDataPropertyExpression> properties = axiom.getProperties();
        if (properties.size() == 2) {
            addPairwise(axiom, properties,
                OWL_EQUIVALENT_PROPERTY.getIRI());
        } else {
            for (OWLEquivalentDataPropertiesAxiom ax : splitToAnnotatedPairs(axiom)) {
                ax.accept(this);
            }
        }
    }

    @Override
    public void visit(OWLDisjointDataPropertiesAxiom axiom) {
        if (axiom.getProperties().size() == 2) {
            addPairwise(axiom, axiom.getProperties(),
                OWL_PROPERTY_DISJOINT_WITH.getIRI());
        } else {
            translateAnonymousNode(axiom);
            translateAnnotations(axiom);
            addTriple(axiom, RDF_TYPE.getIRI(),
                OWL_ALL_DISJOINT_PROPERTIES.getIRI());
            addListTriples(axiom, OWL_MEMBERS.getIRI(), axiom.getProperties());
        }
    }

    @Override
    public void visit(OWLDataPropertyDomainAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDFS_DOMAIN.getIRI(),
            axiom.getDomain());
    }

    @Override
    public void visit(OWLDataPropertyRangeAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDFS_RANGE.getIRI(),
            axiom.getRange());
    }

    @Override
    public void visit(OWLFunctionalDataPropertyAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDF_TYPE.getIRI(),
            OWL_FUNCTIONAL_PROPERTY.getIRI());
    }

    @Override
    public void visit(OWLDatatypeDefinitionAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getDatatype(),
            OWL_EQUIVALENT_CLASS.getIRI(), axiom.getDataRange());
    }

    @Override
    public void visit(OWLHasKeyAxiom axiom) {
        List<OWLPropertyExpression<?, ?>> propertyExpressions = new ArrayList<OWLPropertyExpression<?, ?>>(axiom
            .getPropertyExpressions());
        Collections.sort(propertyExpressions);
        addSingleTripleAxiom(axiom, axiom.getClassExpression(),
            OWL_HAS_KEY.getIRI(), propertyExpressions);
    }

    @Override
    public void visit(OWLSameIndividualAxiom axiom) {
        addPairwise(axiom, axiom.getIndividuals(), OWL_SAME_AS.getIRI());
        processIfAnonymous(axiom.getIndividuals(), axiom);
    }

    @Override
    public void visit(OWLDifferentIndividualsAxiom axiom) {
        translateAnonymousNode(axiom);
        addTriple(axiom, RDF_TYPE.getIRI(), OWL_ALL_DIFFERENT.getIRI());
        addListTriples(axiom, OWL_DISTINCT_MEMBERS.getIRI(),
            axiom.getIndividuals());
        processIfAnonymous(axiom.getIndividuals(), axiom);
    }

    @Override
    public void visit(OWLClassAssertionAxiom axiom) {
        axiom.getIndividual().accept(this);
        addSingleTripleAxiom(axiom, axiom.getIndividual(), RDF_TYPE.getIRI(),
            axiom.getClassExpression());
        processIfAnonymous(axiom.getIndividual(), axiom);
    }

    @Override
    public void visit(OWLObjectPropertyAssertionAxiom axiom) {
        OWLObjectPropertyAssertionAxiom simplified = axiom.getSimplified();
        addSingleTripleAxiom(simplified, simplified.getSubject(),
            simplified.getProperty(), simplified.getObject());
        processIfAnonymous(simplified.getObject(), axiom);
        processIfAnonymous(simplified.getSubject(), axiom);
    }

    @Override
    public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        translateAnonymousNode(axiom);
        addTriple(axiom, RDF_TYPE.getIRI(),
            OWL_NEGATIVE_PROPERTY_ASSERTION.getIRI());
        addTriple(axiom, OWL_SOURCE_INDIVIDUAL.getIRI(), axiom.getSubject());
        addTriple(axiom, OWL_ASSERTION_PROPERTY.getIRI(), axiom.getProperty());
        addTriple(axiom, OWL_TARGET_INDIVIDUAL.getIRI(), axiom.getObject());
        translateAnnotations(axiom);
        processIfAnonymous(axiom.getSubject(), axiom);
        processIfAnonymous(axiom.getObject(), axiom);
    }

    @Override
    public void visit(OWLDataPropertyAssertionAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getSubject(), axiom.getProperty(),
            axiom.getObject());
    }

    @Override
    public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        translateAnonymousNode(axiom);
        addTriple(axiom, RDF_TYPE.getIRI(),
            OWL_NEGATIVE_PROPERTY_ASSERTION.getIRI());
        addTriple(axiom, OWL_SOURCE_INDIVIDUAL.getIRI(), axiom.getSubject());
        addTriple(axiom, OWL_ASSERTION_PROPERTY.getIRI(), axiom.getProperty());
        addTriple(axiom, OWL_TARGET_VALUE.getIRI(), axiom.getObject());
        translateAnnotations(axiom);
        processIfAnonymous(axiom.getSubject(), axiom);
    }

    @Override
    public void visit(OWLAnnotationAssertionAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getSubject(), axiom.getProperty(),
            axiom.getValue());
        if (axiom.getValue() instanceof OWLAnonymousIndividual) {
            processIfAnonymous((OWLAnonymousIndividual) axiom.getValue(), axiom);
        }
    }

    @Override
    public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getSubProperty(),
            RDFS_SUB_PROPERTY_OF.getIRI(), axiom.getSuperProperty());
    }

    @Override
    public void visit(OWLAnnotationPropertyDomainAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDFS_DOMAIN.getIRI(),
            axiom.getDomain());
    }

    @Override
    public void visit(OWLAnnotationPropertyRangeAxiom axiom) {
        addSingleTripleAxiom(axiom, axiom.getProperty(), RDFS_RANGE.getIRI(),
            axiom.getRange());
    }

    @Override
    public void visit(OWLClass desc) {
        if (!nodeMap.containsKey(desc)) {
            nodeMap.put(desc, getResourceNode(desc.getIRI()));
        }
        addStrongTyping(desc);
    }

    @Override
    public void visit(OWLDatatype node) {
        if (!nodeMap.containsKey(node)) {
            nodeMap.put(node, getResourceNode(node.getIRI()));
        }
        addStrongTyping(node);
    }

    @Override
    public void visit(OWLFacetRestriction node) {
        translateAnonymousNode(node);
        addTriple(node, node.getFacet().getIRI(), node.getFacetValue());
    }

    @Override
    public void visit(IRI iri) {
        if (!nodeMap.containsKey(iri)) {
            nodeMap.put(iri, getResourceNode(iri));
        }
    }

    @Override
    public void visit(OWLLiteral node) {
        nodeMap.put(node, getLiteralNode(node));
    }

    @Override
    public void visit(OWLDataProperty property) {
        if (!nodeMap.containsKey(property)) {
            nodeMap.put(property, getPredicateNode(property.getIRI()));
        }
        addStrongTyping(property);
    }

    @Override
    public void visit(OWLObjectProperty property) {
        if (!nodeMap.containsKey(property)) {
            nodeMap.put(property, getPredicateNode(property.getIRI()));
        }
        addStrongTyping(property);
    }

    @Override
    public void visit(OWLAnnotationProperty property) {
        if (!nodeMap.containsKey(property)) {
            nodeMap.put(property, getPredicateNode(property.getIRI()));
        }
        addStrongTyping(property);
    }

    @Override
    public void visit(OWLNamedIndividual individual) {
        if (!nodeMap.containsKey(individual)) {
            nodeMap.put(individual, getResourceNode(individual.getIRI()));
        }
        addStrongTyping(individual);
    }

    @Override
    public void visit(OWLAnonymousIndividual individual) {
        translateAnonymousNode(individual);
    }

    @Override
    public void visit(OWLOntology ontologyToVisit) {
        if (ontologyToVisit.isAnonymous()) {
            translateAnonymousNode(ontologyToVisit);
        } else {
            if (!nodeMap.containsKey(ontologyToVisit)) {
                nodeMap.put(ontologyToVisit, getResourceNode(ontologyToVisit
                    .getOntologyID().getOntologyIRI()));
            }
        }
        addTriple(ontologyToVisit, RDF_TYPE.getIRI(), OWL_ONTOLOGY.getIRI());
    }

    @Override
    public void visit(SWRLRule rule) {
        translateAnonymousNode(rule);
        translateAnnotations(rule);
        addTriple(rule, RDF_TYPE.getIRI(), IMP.getIRI());
        Set<SWRLAtom> antecedent = rule.getBody();
        addTriple(rule, BODY.getIRI(), antecedent, ATOM_LIST.getIRI());
        Set<SWRLAtom> consequent = rule.getHead();
        addTriple(rule, HEAD.getIRI(), consequent, ATOM_LIST.getIRI());
    }

    @Override
    public void visit(SWRLClassAtom node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), CLASS_ATOM.getIRI());
        node.getPredicate().accept(this);
        addTriple(node, CLASS_PREDICATE.getIRI(), node.getPredicate());
        node.getArgument().accept((SWRLObjectVisitor) this);
        addTriple(node, ARGUMENT_1.getIRI(), node.getArgument());
    }

    @Override
    public void visit(SWRLDataRangeAtom node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), DATA_RANGE_ATOM.getIRI());
        node.getPredicate().accept(this);
        addTriple(node, DATA_RANGE.getIRI(), node.getPredicate());
        node.getArgument().accept((SWRLObjectVisitor) this);
        addTriple(node, ARGUMENT_1.getIRI(), node.getArgument());
    }

    @Override
    public void visit(SWRLObjectPropertyAtom node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), INDIVIDUAL_PROPERTY_ATOM.getIRI());
        node.getPredicate().accept(this);
        addTriple(node, PROPERTY_PREDICATE.getIRI(), node.getPredicate());
        node.getFirstArgument().accept((SWRLObjectVisitor) this);
        addTriple(node, ARGUMENT_1.getIRI(), node.getFirstArgument());
        node.getSecondArgument().accept((SWRLObjectVisitor) this);
        addTriple(node, ARGUMENT_2.getIRI(), node.getSecondArgument());
    }

    @Override
    public void visit(SWRLDataPropertyAtom node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), DATAVALUED_PROPERTY_ATOM.getIRI());
        node.getPredicate().accept(this);
        addTriple(node, PROPERTY_PREDICATE.getIRI(), node.getPredicate());
        node.getFirstArgument().accept((SWRLObjectVisitor) this);
        addTriple(node, ARGUMENT_1.getIRI(), node.getFirstArgument());
        node.getSecondArgument().accept((SWRLObjectVisitor) this);
        addTriple(node, ARGUMENT_2.getIRI(), node.getSecondArgument());
    }

    @Override
    public void visit(SWRLBuiltInAtom node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), BUILT_IN_ATOM.getIRI());
        addTriple(node, BUILT_IN.getIRI(), node.getPredicate());
        addTriple(getResourceNode(node.getPredicate()),
            getPredicateNode(RDF_TYPE.getIRI()),
            getResourceNode(BUILT_IN_CLASS.getIRI()));
        addTriple(getResourceNode(node), getPredicateNode(ARGUMENTS.getIRI()),
            translateList(new ArrayList<OWLObject>(node.getArguments())));
    }

    @Override
    public void visit(SWRLDifferentIndividualsAtom node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), DIFFERENT_INDIVIDUALS_ATOM.getIRI());
        node.getFirstArgument().accept((SWRLObjectVisitor) this);
        addTriple(node, ARGUMENT_1.getIRI(), node.getFirstArgument());
        node.getSecondArgument().accept((SWRLObjectVisitor) this);
        addTriple(node, ARGUMENT_2.getIRI(), node.getSecondArgument());
    }

    @Override
    public void visit(SWRLSameIndividualAtom node) {
        translateAnonymousNode(node);
        addTriple(node, RDF_TYPE.getIRI(), SAME_INDIVIDUAL_ATOM.getIRI());
        node.getFirstArgument().accept((SWRLObjectVisitor) this);
        addTriple(node, ARGUMENT_1.getIRI(), node.getFirstArgument());
        node.getSecondArgument().accept((SWRLObjectVisitor) this);
        addTriple(node, ARGUMENT_2.getIRI(), node.getSecondArgument());
    }

    @Override
    public void visit(SWRLVariable node) {
        if (!nodeMap.containsKey(node)) {
            nodeMap.put(node, getResourceNode(node.getIRI()));
        }
        if (!ontology.containsIndividualInSignature(node.getIRI())) {
            addTriple(node, RDF_TYPE.getIRI(), VARIABLE.getIRI());
        }
    }

    @Override
    public void visit(SWRLIndividualArgument node) {
        node.getIndividual().accept(this);
        nodeMap.put(node, nodeMap.get(node.getIndividual()));
    }

    @Override
    public void visit(SWRLLiteralArgument node) {
        node.getLiteral().accept(this);
        nodeMap.put(node, nodeMap.get(node.getLiteral()));
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Methods to add triples
    //
    // ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /** Maps Objects to nodes. */
    private Map<OWLObject, NODE> nodeMap = new HashMap<OWLObject, NODE>();

    private void addSingleTripleAxiom(OWLAxiom ax, OWLObject subject, IRI pred,
        OWLObject obj) {
        addSingleTripleAxiom(ax, getResourceNode(subject),
            getPredicateNode(pred), getNode(obj));
    }

    private void addSingleTripleAxiom(OWLAxiom ax, OWLObject subject, IRI pred,
        IRI obj) {
        addSingleTripleAxiom(ax, getResourceNode(subject),
            getPredicateNode(pred), getResourceNode(obj));
    }

    private void addSingleTripleAxiom(OWLAxiom ax, OWLObject subj, IRI pred,
        List<? extends OWLObject> obj) {
        addSingleTripleAxiom(ax, getResourceNode(subj), getPredicateNode(pred),
            translateList(obj));
    }

    private void addSingleTripleAxiom(OWLAxiom ax, OWLObject subj,
        OWLObject pred, OWLObject obj) {
        addSingleTripleAxiom(ax, getResourceNode(subj), getPredicateNode(pred),
            getNode(obj));
    }

    /**
     * Adds the representation of an axiom to the RDF graph where the axiom has
     * a SINGLE MAIN TRIPLE (specified by the subject, predicate, object
     * parameters). The triple specified by the subject, predicate and object
     * parameters will be added to the graph. If the axiom has any annotations
     * on it then extra triples will be added. These will consist of three
     * triples, that "reify" (not in the RDF sense) the specified triple using
     * the OWL 2 annotation vocabulary: owl:annotatedSource,
     * owl:annotatedProperty, owl:annotatedTarget, and other triples to encode
     * the annotations.
     * 
     * @param ax
     *        The axiom that the triple specified as subject, pred, obj
     *        represents.
     * @param subject
     *        The subject of the triple representing the axiom
     * @param predicate
     *        The predicate of the triple representing the axiom
     * @param object
     *        The object of the triple representing the axiom
     */
    private void addSingleTripleAxiom(OWLAxiom ax, RESOURCE subject,
        PREDICATE predicate, NODE object) {
        // Base triple
        addTriple(subject, predicate, object);
        if (ax.getAnnotations().isEmpty()) {
            return;
        }
        // The axiom has annotations and we therefore need to reify the axiom in
        // order to add the annotations
        translateAnonymousNode(ax);
        addTriple(getResourceNode(ax), getPredicateNode(RDF_TYPE.getIRI()),
            getResourceNode(OWL_AXIOM.getIRI()));
        addTriple(getResourceNode(ax),
            getPredicateNode(OWL_ANNOTATED_SOURCE.getIRI()), subject);
        addTriple(getResourceNode(ax),
            getPredicateNode(OWL_ANNOTATED_PROPERTY.getIRI()), predicate);
        addTriple(getResourceNode(ax),
            getPredicateNode(OWL_ANNOTATED_TARGET.getIRI()), object);
        translateAnnotations(ax);
    }

    private void translateAnnotations(OWLAxiom ax) {
        translateAnonymousNode(ax);
        for (OWLAnnotation anno : new TreeSet<OWLAnnotation>(ax.getAnnotations())) {
            translateAnnotation(ax, anno);
        }
    }

    /**
     * Translates an annotation on a given subject. This method implements the
     * TANN(ann, y) translation in the spec
     * 
     * @param subject
     *        The subject of the annotation
     * @param annotation
     *        The annotation
     */
    private void translateAnnotation(OWLObject subject, OWLAnnotation annotation) {
        // We first add the base triple
        addTriple(subject, annotation.getProperty().getIRI(),
            annotation.getValue());
        // if the annotation has a blank node as subject, add the triples here
        if (annotation.getValue() instanceof OWLAnonymousIndividual) {
            OWLAnonymousIndividual ind = (OWLAnonymousIndividual) annotation
                .getValue();
            translateAnonymousNode(ind);
            processIfAnonymous(ind);
        }
        // If the annotation doesn't have annotations on it then we're done
        Set<OWLAnnotation> annotations = annotation.getAnnotations();
        if (annotations.isEmpty()) {
            return;
        }
        // The annotation has annotations on it so we need to reify the
        // annotation
        // The main node is the annotation, it is typed as an annotation
        translateAnonymousNode(annotation);
        addTriple(annotation, RDF_TYPE.getIRI(), OWL_ANNOTATION.getIRI());
        addTriple(annotation, OWL_ANNOTATED_SOURCE.getIRI(), subject);
        addTriple(annotation, OWL_ANNOTATED_PROPERTY.getIRI(), annotation
            .getProperty().getIRI());
        addTriple(annotation, OWL_ANNOTATED_TARGET.getIRI(),
            annotation.getValue());
        for (OWLAnnotation anno : new TreeSet<OWLAnnotation>(annotations)) {
            translateAnnotation(annotation, anno);
        }
    }

    @Override
    public void visit(OWLAnnotation node) {
        throw new OWLRuntimeException(
            "The translator should not be used directly on instances of OWLAnnotation because an annotation cannot be translated without a subject.");
    }

    private void translateAnonymousNode(OWLObject object) {
        nodeMap.put(object, getAnonymousNode(object));
    }

    /**
     * @param object
     *        that has already been mapped
     * @param <T>
     *        type needed
     * @return mapped node, or null if the node is absent
     */
    public <T extends NODE> T getMappedNode(OWLObject object) {
        return (T) nodeMap.get(object);
    }

    /**
     * Gets a resource that has a IRI.
     * 
     * @param IRI
     *        The IRI of the resource
     * @return The resource with the specified IRI
     */
    protected abstract RESOURCE getResourceNode(IRI IRI);

    protected abstract PREDICATE getPredicateNode(IRI IRI);

    /**
     * Gets an anonymous resource.
     * 
     * @param key
     *        A key for the resource. For a given key identity, the resources
     *        that are returned should be equal and have the same hashcode.
     * @return The resource
     */
    protected abstract RESOURCE getAnonymousNode(Object key);

    protected abstract LITERAL getLiteralNode(OWLLiteral literal);

    protected abstract void addTriple(RESOURCE subject, PREDICATE pred,
        NODE object);

    private RESOURCE getResourceNode(OWLObject object) {
        RESOURCE r = (RESOURCE) nodeMap.get(object);
        if (r == null) {
            object.accept(this);
            r = (RESOURCE) nodeMap.get(object);
            if (r == null) {
                throw new IllegalStateException("Node is null!");
            }
        }
        return r;
    }

    private PREDICATE getPredicateNode(OWLObject object) {
        PREDICATE p = (PREDICATE) nodeMap.get(object);
        if (p == null) {
            object.accept(this);
            p = (PREDICATE) nodeMap.get(object);
            if (p == null) {
                throw new IllegalStateException("Node is null!");
            }
        }
        return p;
    }

    private NODE getNode(OWLObject obj) {
        NODE node = nodeMap.get(obj);
        if (node == null) {
            obj.accept(this);
            node = nodeMap.get(obj);
            if (node == null) {
                throw new IllegalStateException(
                    "Node is null. Attempting to get node for " + obj);
            }
        }
        return node;
    }

    private RESOURCE translateList(List<? extends OWLObject> list) {
        return translateList(list, RDF_LIST.getIRI());
    }

    private RESOURCE translateList(List<? extends OWLObject> list, IRI listType) {
        if (list.isEmpty()) {
            return getResourceNode(RDF_NIL.getIRI());
        }
        RESOURCE main = getResourceNode(RDF_NIL.getIRI());
        int listSize = list.size() - 1;
        for (int i = listSize; i >= 0; i--) {
            RESOURCE anonNode = getAnonymousNode(list.subList(i, listSize));
            addTriple(anonNode, getPredicateNode(RDF_TYPE.getIRI()),
                getResourceNode(listType));
            addTriple(anonNode, getPredicateNode(RDF_FIRST.getIRI()),
                getNode(list.get(i)));
            addTriple(anonNode, getPredicateNode(RDF_REST.getIRI()), main);
            main = anonNode;
        }
        return main;
    }

    private void addTriple(OWLObject subject, IRI pred, IRI object) {
        addTriple(getResourceNode(subject), getPredicateNode(pred),
            getResourceNode(object));
    }

    public void addTriple(RESOURCE subject, IRI pred, IRI object) {
        addTriple(subject, getPredicateNode(pred), getResourceNode(object));
    }

    public void addTriple(RESOURCE subject, IRI pred, OWLObject object) {
        addTriple(subject, getPredicateNode(pred),
            getNode(object));
    }

    private void addTriple(OWLObject subject, IRI pred, OWLObject object) {
        addTriple(getResourceNode(subject), getPredicateNode(pred),
            getNode(object));
    }

    private void addListTriples(OWLObject subject, IRI pred,
        Set<? extends OWLObject> objects) {
        List<OWLObject> list = new ArrayList<OWLObject>(objects);
        Collections.sort(list);
        addTriple(getResourceNode(subject), getPredicateNode(pred),
            translateList(list));
    }

    private void addTriple(OWLObject subject, IRI pred,
        Set<? extends OWLObject> objects, IRI listType) {
        addTriple(getResourceNode(subject), getPredicateNode(pred),
            translateList(new ArrayList<OWLObject>(objects), listType));
    }

    private OWLLiteral toTypedConstant(int i) {
        return manager.getOWLDataFactory().getOWLLiteral(
            Integer.toString(i),
            manager.getOWLDataFactory().getOWLDatatype(
                XSDVocabulary.NON_NEGATIVE_INTEGER.getIRI()));
    }

    private void processIfAnonymous(Set<OWLIndividual> inds, OWLAxiom root) {
        for (OWLIndividual ind : new TreeSet<OWLIndividual>(inds)) {
            processIfAnonymous(ind, root);
        }
    }

    private Set<OWLIndividual> currentIndividuals = new HashSet<OWLIndividual>();

    private void processIfAnonymous(OWLIndividual ind, OWLAxiom root) {
        if (!currentIndividuals.contains(ind)) {
            currentIndividuals.add(ind);
            if (ind.isAnonymous()) {
                for (OWLAxiom ax : getSortedIndividualAxioms(ind)) {
                    if (root == null || !root.equals(ax)) {
                        ax.accept(this);
                    }
                }
                for (OWLAnnotationAssertionAxiom ax : getSortedAxioms(getAnnotationAssertionsForAnonymous(ind))) {
                    ax.accept(this);
                }
            }
            currentIndividuals.remove(ind);
        }
    }

    private void processIfAnonymous(OWLIndividual ind) {
        if (!currentIndividuals.contains(ind)) {
            currentIndividuals.add(ind);
            if (ind.isAnonymous()) {
                Set<OWLIndividualAxiom> axioms = getSortedIndividualAxioms(ind);
                for (OWLAxiom ax : axioms) {
                    ax.accept(this);
                }
                for (OWLAnnotationAssertionAxiom ax : getSortedAxioms(getAnnotationAssertionsForAnonymous(ind))) {
                    ax.accept(this);
                }
            }
            currentIndividuals.remove(ind);
        }
    }

    private <T> TreeSet<T> getSortedAxioms(Set<T> axioms) {
        return new TreeSet<T>(axioms);
    }

    private Set<OWLAnnotationAssertionAxiom> getAnnotationAssertionsForAnonymous(OWLIndividual ind) {
        OWLAnonymousIndividual owlAnonymousIndividual = ind.asOWLAnonymousIndividual();
        return ontology.getAnnotationAssertionAxioms(owlAnonymousIndividual);
    }

    private SortedSet<OWLIndividualAxiom> getSortedIndividualAxioms(OWLIndividual ind) {
        return getSortedAxioms(ontology.getAxioms(ind));
    }

    private void addPairwise(OWLAxiom axiom,
        Collection<? extends OWLObject> objects, IRI IRI) {
        List<? extends OWLObject> objectList = new ArrayList<OWLObject>(objects);
        Collections.sort(objectList);
        for (int i = 0; i < objectList.size(); i++) {
            for (int j = i; j < objectList.size(); j++) {
                if (i != j) {
                    addSingleTripleAxiom(axiom, objectList.get(i), IRI,
                        objectList.get(j));
                }
            }
        }
    }

    /**
     * Renders a set of class expressions in a pairwise manner using the
     * specified IRI. It is assumed that the relationship described by the IRI
     * (e.g. disjointWith) is symmetric. The method delegates to the
     * {@code addPairwise} method after sorting the class expressions so that
     * named classes appear first.
     * 
     * @param axiom
     *        The axiom which will dictate which axiom annotation get rendered
     * @param classExpressions
     *        The set of class expressions to be rendered.
     * @param IRI
     *        The IRI which describes the relationship between pairs of class
     *        expressions.
     */
    private void addPairwiseClassExpressions(OWLAxiom axiom,
        Set<OWLClassExpression> classExpressions, IRI IRI) {
        List<OWLClassExpression> classExpressionList = new ArrayList<OWLClassExpression>(
            classExpressions);
        addPairwise(axiom, classExpressionList, IRI);
    }

    /**
     * Adds triples to strong type an entity. Triples are only added if the
     * useStrongTyping flag is set to {@code true} and the entity is not a built
     * in entity.
     * 
     * @param entity
     *        The entity for which strong typing triples should be added.
     */
    private void addStrongTyping(OWLEntity entity) {
        if (!useStrongTyping) {
            return;
        }
        if (!RDFOntologyFormat.isMissingType(entity, ontology)) {
            return;
        }
        addTriple(entity, RDF_TYPE.getIRI(),
            entity.accept(OWLEntityTypeProvider.INSTANCE));
    }

    /** Visits entities and returns their RDF type. */
    private static class OWLEntityTypeProvider implements
        OWLEntityVisitorEx<IRI> {

        public static OWLEntityTypeProvider INSTANCE = new OWLEntityTypeProvider();

        @Override
        public IRI visit(OWLClass cls) {
            return OWL_CLASS.getIRI();
        }

        @Override
        public IRI visit(OWLObjectProperty property) {
            return OWL_OBJECT_PROPERTY.getIRI();
        }

        @Override
        public IRI visit(OWLDataProperty property) {
            return OWL_DATA_PROPERTY.getIRI();
        }

        @Override
        public IRI visit(OWLNamedIndividual individual) {
            return OWL_NAMED_INDIVIDUAL.getIRI();
        }

        @Override
        public IRI visit(OWLDatatype datatype) {
            return RDFS_DATATYPE.getIRI();
        }

        @Override
        public IRI visit(OWLAnnotationProperty property) {
            return OWL_ANNOTATION_PROPERTY.getIRI();
        }
    }
}
