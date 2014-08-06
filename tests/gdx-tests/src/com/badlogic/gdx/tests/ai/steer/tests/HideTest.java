/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.tests.ai.steer.tests;

import com.badlogic.gdx.ai.steer.behaviors.CollisionAvoidance;
import com.badlogic.gdx.ai.steer.behaviors.Hide;
import com.badlogic.gdx.ai.steer.behaviors.Wander;
import com.badlogic.gdx.ai.steer.behaviors.WeightedBlender;
import com.badlogic.gdx.ai.steer.proximities.InfiniteProximity;
import com.badlogic.gdx.ai.steer.proximities.RadiusProximity;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.tests.SteeringBehaviorTest;
import com.badlogic.gdx.tests.ai.steer.SteeringActor;
import com.badlogic.gdx.tests.ai.steer.SteeringTest;
import com.badlogic.gdx.tests.ai.steer.TargetInputProcessor;
import com.badlogic.gdx.utils.Array;

/** A class to test and experiment with the {@link Hide} behavior.
 * 
 * @autor davebaol */
public class HideTest extends SteeringTest {

	private static final float DISTANCE_FROM_BOUNDARY = 35;
	private static final float THREAT_RADIUS = 200;

	Array<SteeringActor> obstacles;

	SteeringActor character;
	SteeringActor target;
	boolean drawDebug;
	ShapeRenderer shapeRenderer;

	Hide<Vector2> hideSB;
	Wander<Vector2> wanderSB;
	boolean hideMode;

	public HideTest (SteeringBehaviorTest container) {
		super(container, "Hide");
	}

	@Override
	public void create (Table table) {
		drawDebug = true;

		shapeRenderer = new ShapeRenderer();

		// Create obstacles
		obstacles = new Array<SteeringActor>();
		for (int i = 0; i < 6; i++) {
			SteeringActor obstacle = new SteeringActor(MathUtils.randomBoolean() ? container.badlogicSmall : container.cloud, false);
			setRandomNonOverlappingPosition(obstacle, obstacles, 100);
			obstacles.add(obstacle);
			table.addActor(obstacle);
		}

		// Create target
		target = new SteeringActor(container.target);
		table.addActor(target);

		// Create hiding character
		character = new SteeringActor(container.greenFish, false) {
			@Override
			public void act (float delta) {
				// Make wander and hide mutually exclusive based on distance from target
				HideTest.this.hideMode = (target.getPosition().dst2(getPosition()) < THREAT_RADIUS * THREAT_RADIUS);
				HideTest.this.hideSB.setEnabled(hideMode);
				HideTest.this.wanderSB.setEnabled(!hideMode);
				super.act(delta);
			}
		};
		character.setMaxSpeed(150);

		// Add collision avoidance, hide and wander behaviors to character
		RadiusProximity<Vector2> radiusProximity = new RadiusProximity<Vector2>(character, obstacles, character.getBoundingRadius()
			+ DISTANCE_FROM_BOUNDARY * .5f);
		CollisionAvoidance<Vector2> collisionAvoidanceSB = new CollisionAvoidance<Vector2>(character, radiusProximity, 500f);
		InfiniteProximity<Vector2> infProximity = new InfiniteProximity<Vector2>(character, obstacles);
		this.hideSB = new Hide<Vector2>(character, target, infProximity, DISTANCE_FROM_BOUNDARY) //
			.setMaxLinearAcceleration(200) //
			.setMaxSpeed(150) //
			.setTimeToTarget(0.1f) //
			.setArrivalTolerance(0.001f) //
			.setDecelerationRadius(80);

		this.wanderSB = new Wander<Vector2>(character) //
			.setMaxLinearAcceleration(30) //
			.setMaxAngularAcceleration(0) // set to 0 because independent facing is off
			.setAlignTolerance(0.001f) //
			.setDecelerationRadius(5) //
			.setMaxRotation(5) //
			.setTimeToTarget(0.1f) //
			.setWanderOffset(60) //
			.setWanderOrientation(10) //
			.setWanderRadius(40) //
			.setWanderRate(MathUtils.PI / 5);

		WeightedBlender<Vector2> prioritySteeringSB = new WeightedBlender<Vector2>(character, Float.POSITIVE_INFINITY,
			Float.POSITIVE_INFINITY) //
			.add(collisionAvoidanceSB, 1) //
			.add(hideSB, 1) //
			.add(wanderSB, 1);

		character.setSteeringBehavior(prioritySteeringSB);

		table.addActor(character);

		inputProcessor = new TargetInputProcessor(target);

		Table detailTable = new Table(container.skin);

		detailTable.row();
		final Label labelMaxLinAcc = new Label("Max.linear.acc.[" + hideSB.getMaxLinearAcceleration() + "]", container.skin);
		detailTable.add(labelMaxLinAcc);
		detailTable.row();
		Slider maxLinAcc = new Slider(0, 2000, 20, false, container.skin);
		maxLinAcc.setValue(hideSB.getMaxLinearAcceleration());
		maxLinAcc.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				Slider slider = (Slider)actor;
				hideSB.setMaxLinearAcceleration(slider.getValue());
				labelMaxLinAcc.setText("Max.linear.acc.[" + slider.getValue() + "]");
			}
		});
		detailTable.add(maxLinAcc);

		detailTable.row();
		final Label labelMaxSpeed = new Label("Max.Speed [" + hideSB.getMaxSpeed() + "]", container.skin);
		detailTable.add(labelMaxSpeed);
		detailTable.row();
		Slider maxSpeed = new Slider(0, 300, 10, false, container.skin);
		maxSpeed.setValue(hideSB.getMaxSpeed());
		maxSpeed.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				Slider slider = (Slider)actor;
				hideSB.setMaxSpeed(slider.getValue());
				character.setMaxSpeed(slider.getValue());
				labelMaxSpeed.setText("Max.Speed [" + slider.getValue() + "]");
			}
		});
		detailTable.add(maxSpeed);

		detailTable.row();
		final Label labelDecelerationRadius = new Label("Deceleration Radius [" + hideSB.getDecelerationRadius() + "]",
			container.skin);
		detailTable.add(labelDecelerationRadius);
		detailTable.row();
		Slider decelerationRadius = new Slider(0, 150, 1, false, container.skin);
		decelerationRadius.setValue(hideSB.getDecelerationRadius());
		decelerationRadius.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				Slider slider = (Slider)actor;
				hideSB.setDecelerationRadius(slider.getValue());
				labelDecelerationRadius.setText("Deceleration Radius [" + slider.getValue() + "]");
			}
		});
		detailTable.add(decelerationRadius);

		detailTable.row();
		final Label labelArrivalTolerance = new Label("Arrival tolerance [" + hideSB.getArrivalTolerance() + "]", container.skin);
		detailTable.add(labelArrivalTolerance);
		detailTable.row();
		Slider arrivalTolerance = new Slider(0, 1, 0.0001f, false, container.skin);
		arrivalTolerance.setValue(hideSB.getArrivalTolerance());
		arrivalTolerance.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				Slider slider = (Slider)actor;
				hideSB.setArrivalTolerance(slider.getValue());
				labelArrivalTolerance.setText("Arrival tolerance [" + slider.getValue() + "]");
			}
		});
		detailTable.add(arrivalTolerance);

		detailTable.row();
		final Label labelTimeToTarget = new Label("Time to Target [" + hideSB.getTimeToTarget() + " sec.]", container.skin);
		detailTable.add(labelTimeToTarget);
		detailTable.row();
		Slider timeToTarget = new Slider(0, 3, 0.1f, false, container.skin);
		timeToTarget.setValue(hideSB.getTimeToTarget());
		timeToTarget.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				Slider slider = (Slider)actor;
				hideSB.setTimeToTarget(slider.getValue());
				labelTimeToTarget.setText("Time to Target [" + slider.getValue() + " sec.]");
			}
		});
		detailTable.add(timeToTarget);

		detailWindow = createDetailWindow(detailTable);

	}

	@Override
	public void render () {
// if (drawDebug) {
// Steerable<Vector2> steerable = characters.get(0);
// shapeRenderer.begin(ShapeType.Line);
// shapeRenderer.setColor(0, 1, 0, 1);
// shapeRenderer.circle(steerable.getPosition().x, steerable.getPosition().y, char0Proximity.getRadius());
// shapeRenderer.end();
// }
		shapeRenderer.begin(ShapeType.Line);
		if (hideMode)
			shapeRenderer.setColor(1, 0, 0, 1);
		else
			shapeRenderer.setColor(0, 1, 0, 1);
		shapeRenderer.circle(character.getPosition().x, character.getPosition().y, THREAT_RADIUS);
		shapeRenderer.end();
	}

	@Override
	public void dispose () {
		shapeRenderer.dispose();
	}

}
