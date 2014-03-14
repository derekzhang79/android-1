/*
 * Copyright (C) 2014 ohmage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ohmage.prompts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.ohmage.app.R;
import org.ohmage.app.SurveyActivity;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cketcham on 12/19/13.
 */
public class BasePrompt implements Prompt {
    public String surveyItemId;
    public String condition;
    public String text;
    public String surveyItemType;

    @Override
    public boolean isSkippable() {
        return true;
    }

    @Override
    public String getId() {
        return surveyItemId;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Fragment getFragment() {
        return MessagePromptFragment.getInstance(getText());
    }

    /**
     * A fragment which just shows the text of the message
     */
    public static class MessagePromptFragment extends SurveyActivity.BasePromptAdapterFragment {

        public static MessagePromptFragment getInstance(String text) {
            if (TextUtils.isEmpty(text))
                return null;

            MessagePromptFragment fragment = new MessagePromptFragment();
            Bundle args = new Bundle();
            args.putString("text", text);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_basic, container, false);
            ((TextView) view.findViewById(R.id.text)).setText(getArguments().getString("text"));
            return view;
        }
    }

    /**
     * A fragment which holds an instance of a prompt
     */
    public static class PromptFragment<T extends Prompt>
            extends SurveyActivity.BasePromptAdapterFragment {

        private T prompt;

        public void setPrompt(T prompt) {
            this.prompt = prompt;
        }

        public T getPrompt() {
            return prompt;
        }

        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_basic, container, false);
            ((TextView) view.findViewById(R.id.text)).setText(getPrompt().getText());
            onCreatePromptView(inflater, view, savedInstanceState);
            return view;
        }

        /**
         * Allow children to inflate their subview into the main view.
         *
         * @param inflater
         * @param container
         * @param savedInstanceState
         */
        public void onCreatePromptView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        }
    }

    public static class AnswerablePromptFragment<T extends AnswerablePrompt>
            extends PromptFragment<T> {

        /**
         * Calculates if the skippable state between two objects might have changed. Typically this occurs when a value is set or cleared.
         *
         * @param o
         * @param n
         * @return
         */
        protected boolean skippableStateChanged(Object o, Object n) {
            return (o != null && !o.equals(n)) || (n != null && !n.equals(o));
        }

        protected void setValue(Object object) {
            boolean notify = skippableStateChanged(getPrompt().value, object);
            getPrompt().value = object;
            if (notify) {
                notifyValidAnswerStateChanged();
            }
        }

        public interface OnValidAnswerStateChangedListener {
            void onValidAnswerStateChanged(AnswerablePrompt prompt);
        }

        private OnValidAnswerStateChangedListener mOnValidAnswerStateChangedListener;

        public void setOnValidAnswerStateChangedListener(
                OnValidAnswerStateChangedListener onValidAnswerStateChangedListener) {
            mOnValidAnswerStateChangedListener = onValidAnswerStateChangedListener;
        }

        private void notifyValidAnswerStateChanged() {
            if (mOnValidAnswerStateChangedListener != null) {
                mOnValidAnswerStateChangedListener.onValidAnswerStateChanged(getPrompt());
            }
        }
    }

    public static class PromptDeserializer implements JsonDeserializer<Prompt> {
        private static Map<String, Class> map = new HashMap<String, Class>();

        static {
            map.put("message", BasePrompt.class);
            map.put("audio_prompt", AudioPrompt.class);
            map.put("image_prompt", ImagePrompt.class);
            map.put("video_prompt", VideoPrompt.class);
            map.put("number_prompt", NumberPrompt.class);
            map.put("remote_activity_prompt", RemotePrompt.class);
            map.put("number_single_choice_prompt", SingleChoicePrompt.class);
            map.put("string_single_choice_prompt", SingleChoicePrompt.class);
            map.put("number_multi_choice_prompt", MultiChoicePrompt.class);
            map.put("string_multi_choice_prompt", MultiChoicePrompt.class);
            map.put("text_prompt", TextPrompt.class);
            map.put("timestamp_prompt", TimestampPrompt.class);
        }

        public BasePrompt deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) {
            JsonObject object = json.getAsJsonObject();
            String type = object.get("survey_item_type").getAsString();
            if (map.containsKey(type)) {
                return context.deserialize(json, map.get(type));
            }
            return null;
        }
    }

    public abstract static class PromptLauncherFragment<T extends AnswerablePrompt>
            extends AnswerablePromptFragment<T>
            implements View.OnClickListener {

        @Override
        public void onCreatePromptView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_launch, container, true);
            Button launch = (Button) view.findViewById(R.id.launch);
            launch.setText(getLaunchButtonText());
            launch.setOnClickListener(this);
        }

        protected abstract String getLaunchButtonText();
    }
}
