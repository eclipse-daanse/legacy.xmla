/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1999-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
 * 
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.common;

import java.util.Enumeration;

import org.eclipse.daanse.olap.api.Walkable;

import mondrian.util.ArrayStack;

/**
 * Walks over a tree, returning nodes in prefix order. Objects which are an
 * instance of {@link Walkable} supply their children using
 * <code>getChildren()</code>; other objects are assumed to have no children.
 *
 * <p>If the tree is modified during the enumeration, strange things may happen.
 *
 * <p>Example use:<code><pre>
 *    Tree t;
 *    Walker w = new Walker(t);
 *    while (w.hasMoreElements()) {
 *      Tree node = (Tree) w.nextNode();
 *      System.out.println(node.toString());
 *    }
 * </pre></code>
 */
public class Walker implements Enumeration {
    /**
     * The active parts of the tree from the root to nextNode are held in a
     * stack.  When the stack is empty, the enumeration finishes.  currentFrame
     * holds the frame of the 'current node' (the node last returned from
     * nextElement()) because it may no longer be on the stack.
     */
    private final ArrayStack<Frame> stack;
    private Frame currentFrame;
    private Object nextNode;

    private class Frame {
        Frame(Frame parent, Object node) {
            this.parent = parent;
            this.node = node;
            this.children = getChildren(node);
            this.childIndex = -1; // haven't visited first child yet
        }
        final Frame parent;
        final Object node;
        final Object[] children;
        int childIndex;
    }

	public Walker(Walkable<?> root) {
		stack = new ArrayStack<>();
		currentFrame = null;
		visit(null, root);
	}

	private void moveToNext() {
		if (stack.isEmpty()) {
			return;
		}
		currentFrame = stack.peek();

		// Unwind stack until we find a level we have not completed.
		do {
			Frame frame = stack.peek();
			if (frame.children != null && ++frame.childIndex < frame.children.length) {
				// Here is an unvisited child. Visit it.
				visit(frame, frame.children[frame.childIndex]);
				return;
			}
			stack.pop();
		} while (!stack.isEmpty());
		nextNode = null;
	}

	private void visit(Frame parent, Object node) {
		nextNode = node;
		stack.add(new Frame(parent, node));
	}

	@Override
	public boolean hasMoreElements() {
		return nextNode != null;
	}

	@Override
	public Object nextElement() {
		moveToNext();
		return currentFrame.node;
	}

	public final Object getParent() {
		return getAncestor(1);
	}

	public final Object getAncestor(int iDepth) {
		Frame f = getAncestorFrame(iDepth);
		return f == null ? null : f.node;
	}

    /** returns the <code>iDepth</code>th ancestor of the current element */
	private Frame getAncestorFrame(int iDepth) {
		for (Frame f = currentFrame; f != null; f = f.parent) {
			if (iDepth-- == 0) {
				return f;
			}
		}
		return null;
	}

    /** Override this function to prune the tree, or to allow objects which are
     * not Walkable to have children. */
	public Object[] getChildren(Object node) {
		if (node instanceof Walkable walkable) {
			return walkable.getChildren();
		} else {
			return null;
		}
	}

}

