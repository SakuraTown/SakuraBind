/*
 * Copyright (C) 2015 Brian Dupras
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package top.iseason.bukkit.sakurabind.cuckoofilter;

import com.google.common.hash.Funnel;

import java.io.Serializable;

interface CuckooStrategy extends Serializable {
    int ordinal();

    <T> boolean add(T object, Funnel<? super T> funnel, CuckooTable table);

    <T> boolean remove(T object, Funnel<? super T> funnel, CuckooTable table);

    <T> boolean contains(T object, Funnel<? super T> funnel, CuckooTable table);

    boolean addAll(CuckooTable thiz, CuckooTable that);

    boolean equivalent(CuckooTable thiz, CuckooTable that);

    boolean containsAll(CuckooTable thiz, CuckooTable that);

    boolean removeAll(CuckooTable thiz, CuckooTable that);
}
