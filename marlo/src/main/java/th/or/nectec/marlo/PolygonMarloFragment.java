/*
 * Copyright (c) 2016 NECTEC
 *   National Electronics and Computer Technology Center, Thailand
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package th.or.nectec.marlo;

import android.os.Bundle;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import th.or.nectec.marlo.model.PolygonData;

import java.util.Stack;

public class PolygonMarloFragment extends MarloFragment {

    private final Stack<PolygonData> multiPolygon = new Stack<>();
    private final PolygonData singlePolygon = new PolygonData();
    private State drawingState = State.BOUNDARY;
    private Mode mode = Mode.SINGLE;
    private PolygonFactory polygonFactory;

    public PolygonMarloFragment() {
        super();
        this.markerFactory = new DefaultPolygonMarkerFactory();
        this.polygonFactory = new DefaultPolygonFactory();
    }

    public static PolygonMarloFragment newInstance(Mode mode) {
        PolygonMarloFragment fragment = new PolygonMarloFragment();
        fragment.mode = mode;
        return fragment;
    }

    public void setPolygonFactory(PolygonFactory polygonFactory) {
        this.polygonFactory = polygonFactory;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ViewUtils.addPolygonToolsMenu(this);
        findViewBy(R.id.marlo_boundary).setVisibility(mode == Mode.MULTI ? View.VISIBLE : View.GONE);
        multiPolygon.push(new PolygonData());
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.marlo_hole) {
            changeToHoleState();
        } else if (view.getId() == R.id.marlo_boundary) {
            multiPolygon.push(new PolygonData());
            changeToBoundary();
        } else if (view.getId() == R.id.marlo_undo) {
            undo();
        } else {
            super.onClick(view);
        }
    }

    private void changeToHoleState() {
        drawingState = State.HOLE;
    }

    private void changeToBoundary() {
        drawingState = State.BOUNDARY;
    }

    public void undo() {
        Stack<Marker> holeMarker = getActivePolygonData().getHole();
        if (!holeMarker.isEmpty()) {
            holeMarker.peek().remove();
            holeMarker.pop();
        } else {
            Stack<Marker> boundary = getActivePolygonData().getBoundary();
            if (!boundary.isEmpty()) {
                changeToBoundary();
                boundary.peek().remove();
                boundary.pop();
            }

            if (boundary.isEmpty() && mode == Mode.MULTI && multiPolygon.size() > 1) {
                multiPolygon.pop();
            }
        }
        PolygonDrawUtils.createPolygon(getGoogleMap(), getActivePolygonData(), polygonFactory.build(this));
    }

    public PolygonData getActivePolygonData() {
        return mode == Mode.SINGLE ? singlePolygon : multiPolygon.peek();
    }

    @Override
    protected void onViewfinderClick(LatLng target) {
        SoundUtility.play(getContext(), R.raw.thumpsoundeffect);

        Marker marker = getGoogleMap().addMarker(markerFactory.build(this, target));
        switch (drawingState) {
            case BOUNDARY:
                getActivePolygonData().getBoundary().push(marker);
                break;
            case HOLE:
                getActivePolygonData().getHole().push(marker);
                break;
        }
        PolygonDrawUtils.createPolygon(getGoogleMap(), getActivePolygonData(), polygonFactory.build(this));
    }

    public State getDrawingState() {
        return drawingState;
    }

    public Stack<PolygonData> getPolygons() {
        return multiPolygon;
    }

    public PolygonData getPolygon() {
        return singlePolygon;
    }

    public enum State {
        BOUNDARY,
        HOLE,
    }

    public enum Mode {
        SINGLE,
        MULTI
    }
}